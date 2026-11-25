package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商户核销预约请求
 */
@Data
public class ReservationVerifyRequest {

    /** 核销码：6 位数字 */
    @NotBlank(message = "核销码不能为空")
    @Pattern(regexp = "\\d{6}", message = "核销码必须为 6 位数字")
    private String verifyCode;
}
