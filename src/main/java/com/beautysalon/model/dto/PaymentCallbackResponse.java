package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付回调处理结果（仅用于服务端日志，不返回给支付平台）
 */
@Data
@Builder
public class PaymentCallbackResponse {

    /** 平台交易号 */
    private String outTradeNo;

    /** 订单号 */
    private String orderNo;

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 预约当前状态 */
    private String status;

    /** 支付金额（分） */
    private Long amountCents;

    /** 本次回调是否真正改变了业务状态（false 表示幂等命中或非成功状态） */
    private boolean changed;
}
