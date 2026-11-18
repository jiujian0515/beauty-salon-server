package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数（DTO，Data Transfer Object，数据传输对象）
 *
 * <p>前端 POST 过来的 JSON 会自动反序列化到这个对象，例如：</p>
 * <pre>
 * { "phone": "13800000003", "password": "123456" }
 * </pre>
 *
 * <p>{@code @NotBlank}：参数校验注解，字符串不能为空（配合 Controller 里的 @Valid 使用）。</p>
 */
@Data
public class LoginRequest {

    /** 手机号（登录账号） */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
