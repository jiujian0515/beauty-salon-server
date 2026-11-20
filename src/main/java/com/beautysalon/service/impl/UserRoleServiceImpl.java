package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.mapper.RoleMapper;
import com.beautysalon.mapper.UserRoleMapper;
import com.beautysalon.model.entity.Role;
import com.beautysalon.model.entity.UserRole;
import com.beautysalon.service.UserRoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户-角色关联实现
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public UserRoleServiceImpl(UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<Role> rolesOf(Long userId) {
        return loadRoles(Collections.singletonList(userId))
                .getOrDefault(userId, Collections.emptyList());
    }

    @Override
    public List<String> roleCodesOf(Long userId) {
        return rolesOf(userId).stream().map(Role::getCode).toList();
    }

    @Override
    public Map<Long, List<Role>> rolesByUserIds(Collection<Long> userIds) {
        return loadRoles(userIds);
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return roleCodesOf(userId).contains(roleCode);
    }

    @Override
    public List<Role> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少选择一个角色");
        }

        // 去重，避免重复插入触发唯一索引
        Set<Long> distinctIds = new LinkedHashSet<>(roleIds);
        List<Role> roles = roleMapper.selectBatchIds(distinctIds);

        if (roles.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的角色");
        }
        return roles;
    }

    @Override
    public void replaceRoles(Long userId, List<Long> roleIds) {
        // 先校验角色是否都合法，避免删掉旧关联后才发现参数错误
        List<Role> roles = resolveRoles(roleIds);

        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );

        for (Role role : roles) {
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            userRoleMapper.insert(link);
        }
    }

    @Override
    public void removeByUserId(Long userId) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
    }

    @Override
    public long countUsersOfRole(Long roleId) {
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)
        );
        return count == null ? 0 : count;
    }

    /**
     * 批量加载「用户 id → 角色列表」映射
     *
     * <p>两次查询搞定：先查关联表，再按角色 id 批量查角色，避免逐用户查询的 N+1 问题。</p>
     */
    private Map<Long, List<Role>> loadRoles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserRole> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds)
        );
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> roleIds = links.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Map<Long, Role> roleMap = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, role -> role));

        Map<Long, List<Role>> result = new HashMap<>();
        links.stream()
                .sorted(Comparator.comparing(UserRole::getRoleId))
                .forEach(link -> {
                    Role role = roleMap.get(link.getRoleId());
                    if (role != null) {
                        result.computeIfAbsent(link.getUserId(), k -> new ArrayList<>()).add(role);
                    }
                });
        return result;
    }
}
