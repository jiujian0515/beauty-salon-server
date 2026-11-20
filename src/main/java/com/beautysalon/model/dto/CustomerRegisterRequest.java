package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 顾客端自助注册请求（无需登录即可调用）
 *
 * <pre>
 * POST /api/v1/customer/auth/register
 * { "phone": "13900000001", "password": "123456", "nickname": "小美" }
 * </pre>
 */
@Data
public class CustomerRegisterRequest {

    /** 手机号（注册后作为登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;
}
