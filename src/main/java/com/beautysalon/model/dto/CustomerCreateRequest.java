package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台新增顾客请求（管理员在后台添加顾客）
 */
@Data
public class CustomerCreateRequest {

    /** 手机号（登录账号，顾客表内唯一） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 初始密码（BCrypt 加密后入库） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /** 姓名/昵称 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名最长 64 个字符")
    private String nickname;

    /** 状态：1启用 0禁用（不传默认 1 启用） */
    private Integer status;
}
