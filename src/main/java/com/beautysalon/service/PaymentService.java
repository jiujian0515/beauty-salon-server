package com.beautysalon.service;

import com.beautysalon.model.dto.PaymentCallbackResponse;
import com.beautysalon.model.dto.PaymentMockPayRequest;
import com.beautysalon.model.dto.PaymentMockPayResponse;

/**
 * 支付服务
 */
public interface PaymentService {

    /**
     * 模拟支付：演示环境直接触发支付成功回调逻辑，幂等。
     */
    PaymentMockPayResponse mockPay(PaymentMockPayRequest request);

    /**
     * 支付平台回调：验签后按 out_trade_no 幂等结算定金与预约状态。
     *
     * @param rawBody   回调原始报文（JSON）
     * @param signature 请求头 X-Pay-Signature（缺省时回退到报文体 sign 字段）
     */
    PaymentCallbackResponse handleCallback(String rawBody, String signature);
}
