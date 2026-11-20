package com.beautysalon.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台编辑顾客请求（局部更新：只改传了值的字段）
 */
@Data
public class CustomerUpdateRequest {

    /** 新手机号（不传表示不改） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 新姓名（不传表示不改） */
    @Size(max = 64, message = "姓名最长 64 个字符")
    private String nickname;

    /** 状态：1启用 0禁用（不传表示不改） */
    private Integer status;
}
