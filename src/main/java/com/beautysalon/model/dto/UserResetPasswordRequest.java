package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置用户密码请求
 */
@Data
public class UserResetPasswordRequest {

    /** 新密码（后端 BCrypt 加密后入库） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String newPassword;
}
