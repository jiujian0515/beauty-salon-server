package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模拟支付请求
 */
@Data
public class PaymentMockPayRequest {

    /** 平台交易号（创建预约时 paymentParams.outTradeNo） */
    @NotBlank(message = "outTradeNo 不能为空")
    private String outTradeNo;
}
