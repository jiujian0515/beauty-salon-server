package com.beautysalon.service;

import com.beautysalon.model.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户-角色关联服务（参考若依 RuoYi 的 sys_user_role）
 *
 * <p>把「用户拥有哪些角色」「给用户分配角色」这类操作集中在这里，
 * 供用户管理、登录鉴权（Sa-Token）、当前用户信息等场景复用。</p>
 */
public interface UserRoleService {

    /** 查询某个用户的角色列表（按角色 id 升序） */
    List<Role> rolesOf(Long userId);

    /** 查询某个用户的角色代码列表（如 ["ADMIN"]） */
    List<String> roleCodesOf(Long userId);

    /** 批量查询多个用户的角色（分页列表用，避免 N+1 查询） */
    Map<Long, List<Role>> rolesByUserIds(Collection<Long> userIds);

    /** 判断用户是否拥有指定角色代码 */
    boolean hasRole(Long userId, String roleCode);

    /**
     * 校验并解析角色 id 列表
     *
     * @param roleIds 角色 id 列表（不能为空，且必须全部存在）
     * @return 解析出的角色列表
     */
    List<Role> resolveRoles(List<Long> roleIds);

    /** 覆盖式分配角色：先清空用户原有角色，再写入新角色 */
    void replaceRoles(Long userId, List<Long> roleIds);

    /** 删除用户的全部角色关联（删除用户时调用） */
    void removeByUserId(Long userId);

    /** 统计某个角色被多少用户使用（删除角色前校验） */
    long countUsersOfRole(Long roleId);
}
