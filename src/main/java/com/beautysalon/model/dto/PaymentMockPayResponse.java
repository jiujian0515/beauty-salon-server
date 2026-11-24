package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟支付返回体
 */
@Data
@Builder
public class PaymentMockPayResponse {

    /** 平台交易号 */
    private String outTradeNo;

    /** 订单号 */
    private String orderNo;

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 预约状态（支付成功后为 CONFIRMED） */
    private String status;

    /** 支付金额（分） */
    private Long amountCents;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;
}
