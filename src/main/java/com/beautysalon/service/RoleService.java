package com.beautysalon.service;

import com.beautysalon.model.dto.RoleCreateRequest;
import com.beautysalon.model.dto.RoleResponse;
import com.beautysalon.model.dto.RoleUpdateRequest;

import java.util.List;

/**
 * 角色管理服务（角色字典的增删改查）
 *
 * <p>角色是全局字典，不区分商户，但仅 ADMIN 角色可管理。</p>
 */
public interface RoleService {

    /** 查询全部角色（按 id 升序，供下拉框 + 管理列表使用） */
    List<RoleResponse> list();

    /** 查询角色详情 */
    RoleResponse detail(Long id);

    /** 新增角色 */
    RoleResponse create(RoleCreateRequest request);

    /** 编辑角色（局部更新） */
    RoleResponse update(Long id, RoleUpdateRequest request);

    /** 删除角色（仍被用户引用时禁止删除） */
    void delete(Long id);
}
