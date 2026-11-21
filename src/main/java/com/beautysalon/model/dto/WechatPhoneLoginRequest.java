package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信手机号快速登录请求
 *
 * <pre>
 * POST /api/v1/customer/auth/wechat/login
 * {
 *   "loginCode": "wx.login() 返回的 code",
 *   "phoneCode": "button open-type=getPhoneNumber 返回的 code"
 * }
 * </pre>
 */
@Data
public class WechatPhoneLoginRequest {

    /** wx.login() 返回的临时登录凭证（用于换取 openid） */
    @NotBlank(message = "微信登录 code 不能为空")
    private String loginCode;

    /** getPhoneNumber 返回的手机号授权 code（用于换取手机号） */
    @NotBlank(message = "微信手机号 code 不能为空")
    private String phoneCode;
}
