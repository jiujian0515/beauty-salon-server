package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ServiceItemCreateRequest;
import com.beautysalon.model.dto.ServiceItemResponse;
import com.beautysalon.model.dto.ServiceItemUpdateRequest;
import com.beautysalon.service.ServiceItemService;
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

/**
 * 服务项目管理接口（商户端，仅管理员可用）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/service-items</td><td>分页列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/service-items/{id}</td><td>详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/service-items</td><td>新增</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/service-items/{id}</td><td>编辑</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/service-items/{id}</td><td>删除</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/service-items")
@SaCheckRole("ADMIN")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    /** 分页查询服务项目列表 */
    @GetMapping
    public Result<PageResult<ServiceItemResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(serviceItemService.page(page, size, keyword, status));
    }

    /** 服务项目详情 */
    @GetMapping("/{id}")
    public Result<ServiceItemResponse> detail(@PathVariable Long id) {
        return Result.ok(serviceItemService.detail(id));
    }

    /** 新增服务项目 */
    @PostMapping
    public Result<ServiceItemResponse> create(@Valid @RequestBody ServiceItemCreateRequest request) {
        return Result.ok(serviceItemService.create(request));
    }

    /** 编辑服务项目（局部更新） */
    @PutMapping("/{id}")
    public Result<ServiceItemResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody ServiceItemUpdateRequest request) {
        return Result.ok(serviceItemService.update(id, request));
    }

    /** 删除服务项目 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return Result.ok();
    }
}
