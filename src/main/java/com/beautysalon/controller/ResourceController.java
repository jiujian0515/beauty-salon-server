package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ResourceCreateRequest;
import com.beautysalon.model.dto.ResourceResponse;
import com.beautysalon.model.dto.ResourceUpdateRequest;
import com.beautysalon.model.dto.StaffCandidateResponse;
import com.beautysalon.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资源管理接口（商户端，仅管理员可用）
 *
 * <p>资源分两类：STAFF 技师 / ROOM 床位。技师必须绑定本商户的「非管理员」用户，
 * 前端通过 {@code /staff-candidates} 获取候选用户列表。</p>
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/resources</td><td>分页列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/resources/staff-candidates</td><td>技师候选用户</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/resources/{id}</td><td>详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/resources</td><td>新增</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/resources/{id}</td><td>编辑</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/resources/{id}</td><td>删除</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/resources")
@SaCheckRole("ADMIN")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 分页查询资源列表
     *
     * <p>示例：GET /api/v1/admin/resources?page=1&size=10&type=STAFF&status=1&keyword=小王</p>
     */
    @GetMapping
    public Result<PageResult<ResourceResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.ok(resourceService.page(page, size, keyword, type, status));
    }

    /**
     * 技师候选用户列表
     *
     * <p>只返回本商户「非管理员、已启用、未被其他资源绑定」的用户。
     * 编辑资源时传 {@code resourceId}，当前已绑定用户仍会保留在列表中。</p>
     */
    @GetMapping("/staff-candidates")
    public Result<List<StaffCandidateResponse>> staffCandidates(
            @RequestParam(required = false) Long resourceId) {
        return Result.ok(resourceService.staffCandidates(resourceId));
    }

    /** 资源详情 */
    @GetMapping("/{id}")
    public Result<ResourceResponse> detail(@PathVariable Long id) {
        return Result.ok(resourceService.detail(id));
    }

    /** 新增资源 */
    @PostMapping
    public Result<ResourceResponse> create(@Valid @RequestBody ResourceCreateRequest request) {
        return Result.ok(resourceService.create(request));
    }

    /** 编辑资源（局部更新） */
    @PutMapping("/{id}")
    public Result<ResourceResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody ResourceUpdateRequest request) {
        return Result.ok(resourceService.update(id, request));
    }

    /** 删除资源 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.ok();
    }
}
