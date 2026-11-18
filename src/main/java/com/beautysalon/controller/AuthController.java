package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.dto.UserInfoResponse;
import com.beautysalon.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（接口层，只做「接收请求 + 校验参数 + 调 service + 返回结果」，不含业务逻辑）
 *
 * <p>注解说明：</p>
 * <ul>
 *   <li>{@code @RestController}：处理 HTTP 请求并返回 JSON。</li>
 *   <li>{@code @RequestMapping("/api/v1/auth")}：本类所有接口的统一前缀。</li>
 *   <li>{@code @PostMapping("/login")}：处理 POST 请求，完整路径 /api/v1/auth/login。</li>
 *   <li>{@code @RequestBody}：把请求体 JSON 反序列化成 LoginRequest 对象。</li>
 *   <li>{@code @Valid}：触发 LoginRequest 里的参数校验注解（如 @NotBlank）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录接口（无需登录即可访问，已在 SaTokenConfig 中放行） */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** 登出接口（需要登录才能访问，token 放在请求头 Authorization） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /**
     * 当前登录用户信息（需要登录）
     *
     * <p>注意这个方法没有任何参数：用户是谁，完全由请求头里的 token 决定。
     * 前端刷新页面后可以调这个接口来恢复登录状态（比把用户信息存 localStorage 更可靠）。</p>
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.currentUser());
    }
}
