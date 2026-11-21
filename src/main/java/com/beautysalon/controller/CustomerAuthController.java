package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.CustomerInfoResponse;
import com.beautysalon.model.dto.CustomerRegisterRequest;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.dto.WechatPhoneLoginRequest;
import com.beautysalon.service.CustomerAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端认证接口（不需要后台权限，顾客自己注册/登录）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th><th>是否需登录</th></tr>
 *   <tr><td>POST</td><td>/api/v1/customer/auth/register</td><td>自助注册（成功即返回 token）</td><td>否</td></tr>
 *   <tr><td>POST</td><td>/api/v1/customer/auth/login</td><td>手机号 + 密码登录</td><td>否</td></tr>
 *   <tr><td>POST</td><td>/api/v1/customer/auth/wechat/login</td><td>微信手机号快速登录</td><td>否</td></tr>
 *   <tr><td>POST</td><td>/api/v1/customer/auth/logout</td><td>登出</td><td>是</td></tr>
 *   <tr><td>GET</td><td>/api/v1/customer/auth/me</td><td>当前顾客信息</td><td>是</td></tr>
 * </table>
 *
 * <p>⚠️ 顾客 token 只能访问顾客端接口；后台接口（/api/v1/admin/**）会因为顾客没有后台角色被拦截。</p>
 */
@RestController
@RequestMapping("/api/v1/customer/auth")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    /** 顾客自助注册（已在 SaTokenConfig 中放行，无需登录） */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return Result.ok(customerAuthService.register(request));
    }

    /** 顾客手机号 + 密码登录（已在 SaTokenConfig 中放行，无需登录） */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(customerAuthService.login(request));
    }

    /**
     * 微信手机号快速登录（已在 SaTokenConfig 中放行，无需登录）
     *
     * <p>前端先调用 wx.login() 拿 loginCode，再通过
     * {@code <button open-type="getPhoneNumber">} 拿 phoneCode，一起提交。</p>
     */
    @PostMapping("/wechat/login")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WechatPhoneLoginRequest request) {
        return Result.ok(customerAuthService.wechatLogin(request));
    }

    /** 顾客登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        customerAuthService.logout();
        return Result.ok();
    }

    /** 当前登录顾客信息（前端刷新页面后用它恢复登录态） */
    @GetMapping("/me")
    public Result<CustomerInfoResponse> me() {
        return Result.ok(customerAuthService.currentCustomer());
    }
}
