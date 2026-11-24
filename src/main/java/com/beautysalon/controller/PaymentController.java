package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.PaymentCallbackResponse;
import com.beautysalon.model.dto.PaymentMockPayRequest;
import com.beautysalon.model.dto.PaymentMockPayResponse;
import com.beautysalon.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端支付接口
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th><th>是否需登录</th></tr>
 *   <tr><td>POST</td><td>/api/v1/payments/mock-pay</td><td>模拟支付（演示触发成功回调）</td><td>是（顾客）</td></tr>
 *   <tr><td>POST</td><td>/api/v1/payments/callback</td><td>支付平台回调（HMAC-SHA256 验签）</td><td>否（签名）</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String CALLBACK_ACK_SUCCESS = "SUCCESS";
    private static final String CALLBACK_ACK_FAIL = "FAIL";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 模拟支付（顾客登录后调用，幂等） */
    @PostMapping("/mock-pay")
    public Result<PaymentMockPayResponse> mockPay(
            @Valid @RequestBody PaymentMockPayRequest request) {
        return Result.ok(paymentService.mockPay(request));
    }

    /**
     * 支付平台回调（匿名，签名校验）。
     *
     * <p>按真实支付平台约定返回纯文本：处理成功应答 {@code SUCCESS}，
     * 任何异常（验签失败、金额不符、状态非法等）应答 {@code FAIL}，
     * 平台收到 FAIL 会按策略重试。异常不向上抛，避免返回 JSON 结构给平台。</p>
     */
    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String callback(@RequestBody(required = false) String body,
                           @RequestHeader(value = "X-Pay-Signature", required = false)
                           String signature) {
        try {
            PaymentCallbackResponse result = paymentService.handleCallback(body, signature);
            log.info("支付回调处理成功：outTradeNo={}, reservationNo={}, status={}, changed={}",
                    result.getOutTradeNo(), result.getReservationNo(),
                    result.getStatus(), result.isChanged());
            return CALLBACK_ACK_SUCCESS;
        } catch (Exception e) {
            log.warn("支付回调处理失败：{}", e.getMessage());
            return CALLBACK_ACK_FAIL;
        }
    }
}
