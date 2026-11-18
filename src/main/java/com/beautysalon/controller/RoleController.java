package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.RoleCreateRequest;
import com.beautysalon.model.dto.RoleResponse;
import com.beautysalon.model.dto.RoleUpdateRequest;
import com.beautysalon.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口（仅管理员可用）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/roles</td><td>角色列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/roles/{id}</td><td>角色详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/roles</td><td>新增角色</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/roles/{id}</td><td>编辑角色</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/roles/{id}</td><td>删除角色</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@SaCheckRole("ADMIN")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** 角色列表（供下拉框 + 管理列表） */
    @GetMapping
    public Result<List<RoleResponse>> list() {
        return Result.ok(roleService.list());
    }

    /** 角色详情 */
    @GetMapping("/{id}")
    public Result<RoleResponse> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    /** 新增角色 */
    @PostMapping
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleService.create(request));
    }

    /** 编辑角色（局部更新） */
    @PutMapping("/{id}")
    public Result<RoleResponse> update(@PathVariable Long id,
                                       @Valid @RequestBody RoleUpdateRequest request) {
        return Result.ok(roleService.update(id, request));
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
