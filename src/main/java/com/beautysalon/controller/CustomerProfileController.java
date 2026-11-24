package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.CustomerProfileResponse;
import com.beautysalon.service.CustomerProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端个人信息接口
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th><th>是否需登录</th></tr>
 *   <tr><td>GET</td><td>/api/v1/customer/profile</td><td>个人信息 + 预约统计</td><td>是（顾客）</td></tr>
 * </table>
 *
 * <p>预约统计包含：累计成功预约（已完成）、待服务、待支付。
 * 与 {@code GET /api/v1/customer/auth/me} 的区别：/me 只返回账号资料，本接口额外返回统计。</p>
 */
@RestController
@RequestMapping("/api/v1/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    /** 当前登录顾客的个人信息与预约统计 */
    @GetMapping
    public Result<CustomerProfileResponse> profile() {
        return Result.ok(customerProfileService.currentProfile());
    }
}
