package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.CustomerCreateRequest;
import com.beautysalon.model.dto.CustomerResetPasswordRequest;
import com.beautysalon.model.dto.CustomerResponse;
import com.beautysalon.model.dto.CustomerUpdateRequest;
import com.beautysalon.service.CustomerService;
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
 * 顾客管理接口（后台使用，仅管理员可用）
 *
 * <p>顾客是全局共享的（不绑定商户），所以这里没有 merchant 过滤。</p>
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/customers</td><td>分页列表（手机号/姓名模糊查询）</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/customers/{id}</td><td>详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/customers</td><td>新增顾客</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/customers/{id}</td><td>编辑顾客</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/customers/{id}</td><td>删除顾客</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/customers/{id}/password</td><td>重置密码</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/customers")
@SaCheckRole("ADMIN")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** 分页查询顾客，示例：GET /api/v1/admin/customers?page=1&size=10&keyword=小美 */
    @GetMapping
    public Result<PageResult<CustomerResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(customerService.page(page, size, keyword));
    }

    /** 顾客详情 */
    @GetMapping("/{id}")
    public Result<CustomerResponse> detail(@PathVariable Long id) {
        return Result.ok(customerService.detail(id));
    }

    /** 新增顾客（后台添加的顾客同样可以登录顾客端） */
    @PostMapping
    public Result<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        return Result.ok(customerService.create(request));
    }

    /** 编辑顾客（局部更新：只传要改的字段） */
    @PutMapping("/{id}")
    public Result<CustomerResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody CustomerUpdateRequest request) {
        return Result.ok(customerService.update(id, request));
    }

    /** 删除顾客（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }

    /** 重置顾客密码 */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody CustomerResetPasswordRequest request) {
        customerService.resetPassword(id, request);
        return Result.ok();
    }
}
