package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.mapper.RoleMapper;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.RoleCreateRequest;
import com.beautysalon.model.dto.RoleResponse;
import com.beautysalon.model.dto.RoleUpdateRequest;
import com.beautysalon.model.entity.Role;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色管理实现
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    public RoleServiceImpl(RoleMapper roleMapper, UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<RoleResponse> list() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getId)
        );
        return roles.stream().map(this::toResponse).toList();
    }

    @Override
    public RoleResponse detail(Long id) {
        return toResponse(findRole(id));
    }

    @Override
    public RoleResponse create(RoleCreateRequest request) {
        // 角色代码全库唯一
        Long exists = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, request.getCode())
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "角色代码已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        roleMapper.insert(role);
        return toResponse(role);
    }

    @Override
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = findRole(id);

        // 改代码前查重（排除自己）
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(role.getCode())) {
            Long exists = roleMapper.selectCount(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, request.getCode())
            );
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_DATA, "角色代码已存在");
            }
            role.setCode(request.getCode());
        }
        if (StringUtils.hasText(request.getName())) {
            role.setName(request.getName());
        }

        roleMapper.updateById(role);
        return toResponse(role);
    }

    @Override
    public void delete(Long id) {
        Role role = findRole(id);

        // 角色仍被用户引用时禁止删除，避免留下无法鉴权的孤儿账号
        Long refCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, role.getCode())
        );
        if (refCount != null && refCount > 0) {
            throw new BusinessException(ErrorCode.IN_USE, "该角色下仍有用户，无法删除");
        }

        roleMapper.deleteById(id);
    }

    private Role findRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
