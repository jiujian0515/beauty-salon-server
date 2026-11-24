package com.beautysalon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置（app.payment.*）
 *
 * <p>回调签名密钥属于敏感信息，生产环境通过环境变量 PAYMENT_CALLBACK_SECRET 注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    /** 是否开启模拟支付（演示环境） */
    private boolean mock = true;

    /** 待支付占位时限（分钟） */
    private int expireMinutes = 120;

    /** 可取消提前量（小时） */
    private int cancelAheadHours = 24;

    /** 支付平台回调签名密钥（HMAC-SHA256） */
    private String callbackSecret;
}
