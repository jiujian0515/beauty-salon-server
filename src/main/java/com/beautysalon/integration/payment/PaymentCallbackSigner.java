package com.beautysalon.integration.payment;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.config.PaymentProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付回调验签器（HMAC-SHA256）
 *
 * <p>签名算法：取回调报文中的全部字段（排除 sign / signature），按 key 字典序升序，
 * 拼成 {@code k1=v1&k2=v2} 的待签名串，再用共享密钥做 HMAC-SHA256，
 * 输出小写十六进制摘要，与 {@code X-Pay-Signature} 请求头（或报文体 sign 字段）比对。</p>
 */
@Component
public class PaymentCallbackSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGN_FIELD = "sign";
    private static final String SIGNATURE_FIELD = "signature";

    private final PaymentProperties paymentProperties;

    public PaymentCallbackSigner(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    /** 校验回调签名，失败抛业务异常（由 Controller 转成纯文本 FAIL） */
    public void verify(Map<String, Object> params, String signature) {
        String secret = paymentProperties.getCallbackSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付回调密钥未配置");
        }

        String provided = StringUtils.hasText(signature)
                ? signature
                : params.get(SIGN_FIELD) instanceof String sign ? sign : null;
        if (!StringUtils.hasText(provided)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少回调签名");
        }

        String expected = sign(params, secret);
        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回调签名校验失败");
        }
    }

    /** 按约定算法计算签名（小写十六进制） */
    public String sign(Map<String, Object> params, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(canonical(params).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "回调签名计算失败");
        }
    }

    private String canonical(Map<String, Object> params) {
        return params.entrySet().stream()
                .filter(entry -> !SIGN_FIELD.equals(entry.getKey())
                        && !SIGNATURE_FIELD.equals(entry.getKey()))
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(entry.getKey(), scalar(entry.getValue())))
                .filter(entry -> !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String scalar(Object value) {
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "回调参数仅支持一层键值对");
    }
}
