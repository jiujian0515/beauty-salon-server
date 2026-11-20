package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.mapper.RoleMapper;
import com.beautysalon.model.dto.RoleCreateRequest;
import com.beautysalon.model.dto.RoleResponse;
import com.beautysalon.model.dto.RoleUpdateRequest;
import com.beautysalon.model.entity.Role;
import com.beautysalon.service.RoleService;
import com.beautysalon.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色管理实现
 *
 * <p>角色是全局字典（所有商户共用一套），仅 ADMIN 角色可维护。</p>
 *
 * <p>内置管理员角色（code = ADMIN）受保护：</p>
 * <ul>
 *   <li>不允许再创建一个 code 为 ADMIN 的角色（唯一键也会拦，这里给出更友好的提示）</li>
 *   <li>不允许修改（改名/改代码都会让系统失去「管理员」这个权限锚点）</li>
 *   <li>不允许删除（删了就没有人能管理系统了）</li>
 * </ul>
 *
 * <p>CUSTOMER 是顾客端保留代码：顾客已独立成 customer 表、不参与后台角色体系，
 * 所以角色管理里不允许创建或改成 CUSTOMER。</p>
 */
@Service
public class RoleServiceImpl implements RoleService {

    /** 内置管理员角色代码（受保护，不可创建/修改/删除） */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 顾客保留代码（顾客不走后台角色，禁止创建） */
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final RoleMapper roleMapper;
    private final UserRoleService userRoleService;

    public RoleServiceImpl(RoleMapper roleMapper, UserRoleService userRoleService) {
        this.roleMapper = roleMapper;
        this.userRoleService = userRoleService;
    }

    @Override
    public List<RoleResponse> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))
                .stream()
                .map(RoleResponse::of)
                .toList();
    }

    @Override
    public RoleResponse detail(Long id) {
        return RoleResponse.of(findRole(id));
    }

    @Override
    public RoleResponse create(RoleCreateRequest request) {
        if (ROLE_ADMIN.equals(request.getCode())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员角色为内置角色，不允许创建");
        }
        if (ROLE_CUSTOMER.equals(request.getCode())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "CUSTOMER 是顾客专属角色，不允许创建");
        }

        Long exists = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, request.getCode())
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该角色代码已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        roleMapper.insert(role);
        // 重新查一次，把数据库默认值（created_at）带给前端
        return RoleResponse.of(findRole(role.getId()));
    }

    @Override
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = findRole(id);
        if (ROLE_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员角色不允许修改");
        }

        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(role.getCode())) {
            if (ROLE_ADMIN.equals(request.getCode())) {
                // 不允许把别的角色改成 ADMIN，否则会出现两个「管理员」
                throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员角色代码不允许被占用");
            }
            if (ROLE_CUSTOMER.equals(request.getCode())) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "CUSTOMER 是顾客专属角色，不允许使用");
            }
            Long exists = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                    .eq(Role::getCode, request.getCode())
                    .ne(Role::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该角色代码已存在");
            }
            role.setCode(request.getCode());
        }

        if (StringUtils.hasText(request.getName())) {
            role.setName(request.getName());
        }

        roleMapper.updateById(role);
        return RoleResponse.of(role);
    }

    @Override
    public void delete(Long id) {
        Role role = findRole(id);
        if (ROLE_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员角色不允许删除");
        }

        // 仍被用户引用时不允许删除，否则这些用户会变成「无角色」的孤儿账号
        if (userRoleService.countUsersOfRole(id) > 0) {
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
}
