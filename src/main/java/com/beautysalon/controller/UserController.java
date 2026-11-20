package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.UserCreateRequest;
import com.beautysalon.model.dto.UserResetPasswordRequest;
import com.beautysalon.model.dto.UserResponse;
import com.beautysalon.model.dto.UserUpdateRequest;
import com.beautysalon.service.UserService;
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
 * 用户管理接口（商户端，仅管理员可用）
 *
 * <p>只管理后台账号：管理员 / 员工。顾客是独立账号体系，
 * 请使用 {@link CustomerController}（/api/v1/admin/customers）。</p>
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/users</td><td>分页列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/users/{id}</td><td>详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/users</td><td>新增</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/users/{id}</td><td>编辑</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/users/{id}</td><td>删除</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/users/{id}/password</td><td>重置密码</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@SaCheckRole("ADMIN")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表
     *
     * <p>示例：GET /api/v1/admin/users?page=1&size=10&keyword=小王&role=STAFF</p>
     */
    @GetMapping
    public Result<PageResult<UserResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return Result.ok(userService.page(page, size, keyword, role));
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public Result<UserResponse> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    /** 新增用户（创建后即可用手机号 + 密码登录） */
    @PostMapping
    public Result<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /** 编辑用户（局部更新：只传要改的字段） */
    @PutMapping("/{id}")
    public Result<UserResponse> update(@PathVariable Long id,
                                       @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    /** 删除用户（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /** 重置用户密码 */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody UserResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return Result.ok();
    }
}
