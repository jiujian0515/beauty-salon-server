package com.beautysalon.service.impl;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.CustomerContext;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.integration.payment.PaymentCallbackSigner;
import com.beautysalon.mapper.AuditLogMapper;
import com.beautysalon.mapper.DepositTransactionMapper;
import com.beautysalon.mapper.ReservationMapper;
import com.beautysalon.model.dto.PaymentCallbackResponse;
import com.beautysalon.model.dto.PaymentMockPayRequest;
import com.beautysalon.model.dto.PaymentMockPayResponse;
import com.beautysalon.model.entity.AuditLog;
import com.beautysalon.model.entity.Customer;
import com.beautysalon.model.entity.DepositTransaction;
import com.beautysalon.model.entity.Reservation;
import com.beautysalon.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付服务实现
 *
 * <p>模拟支付与支付平台回调共用同一套「支付成功结算」逻辑：以 out_trade_no 为幂等键，
 * 原子流转定金流水与预约单状态。</p>
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String TX_PENDING = "PENDING";
    private static final String TX_SUCCESS = "SUCCESS";
    private static final String CHANNEL_MOCK = "MOCK";
    private static final String RES_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String RES_CONFIRMED = "CONFIRMED";
    private static final String TRADE_SUCCESS = "SUCCESS";
    private static final String ACTION_PAY_RESERVATION = "PAY_RESERVATION";
    private static final String TARGET_RESERVATION = "RESERVATION";
    private static final String FIELD_OUT_TRADE_NO = "outTradeNo";
    private static final String FIELD_TRADE_STATUS = "tradeStatus";
    private static final String FIELD_AMOUNT_CENTS = "amountCents";

    private final DepositTransactionMapper depositTransactionMapper;
    private final ReservationMapper reservationMapper;
    private final AuditLogMapper auditLogMapper;
    private final CustomerContext customerContext;
    private final PaymentCallbackSigner callbackSigner;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(DepositTransactionMapper depositTransactionMapper,
                              ReservationMapper reservationMapper,
                              AuditLogMapper auditLogMapper,
                              CustomerContext customerContext,
                              PaymentCallbackSigner callbackSigner,
                              ObjectMapper objectMapper) {
        this.depositTransactionMapper = depositTransactionMapper;
        this.reservationMapper = reservationMapper;
        this.auditLogMapper = auditLogMapper;
        this.customerContext = customerContext;
        this.callbackSigner = callbackSigner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentMockPayResponse mockPay(PaymentMockPayRequest request) {
        Customer customer = customerContext.currentCustomer();

        DepositTransaction transaction =
                depositTransactionMapper.selectByOutTradeNo(request.getOutTradeNo());
        if (transaction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付订单不存在");
        }
        if (!CHANNEL_MOCK.equals(transaction.getChannel())) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "仅支持模拟支付订单");
        }

        Reservation reservation = reservationMapper.selectById(transaction.getReservationId());
        if (reservation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约单不存在");
        }
        if (!Objects.equals(reservation.getCustomerId(), customer.getId())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权操作该订单");
        }

        settleSuccess(transaction, reservation, "模拟支付");

        DepositTransaction latestTransaction =
                depositTransactionMapper.selectByOutTradeNo(transaction.getOutTradeNo());
        Reservation latestReservation = reservationMapper.selectById(reservation.getId());
        return buildResponse(latestTransaction, latestReservation);
    }

    @Override
    @Transactional
    public PaymentCallbackResponse handleCallback(String rawBody, String signature) {
        Map<String, Object> params = readParams(rawBody);
        // 先验签再碰数据库：回调入口是公网匿名接口，签名是唯一凭证
        callbackSigner.verify(params, signature);

        String outTradeNo = requireText(params, FIELD_OUT_TRADE_NO);
        String tradeStatus = requireText(params, FIELD_TRADE_STATUS);
        Long amountCents = requireAmount(params);

        DepositTransaction transaction = depositTransactionMapper.selectByOutTradeNo(outTradeNo);
        if (transaction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付订单不存在");
        }
        if (!Objects.equals(transaction.getAmountCents(), amountCents)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回调金额与订单金额不一致");
        }

        Reservation reservation = reservationMapper.selectById(transaction.getReservationId());
        if (reservation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约单不存在");
        }

        // 支付平台可能推送关闭/失败等状态：不改变业务状态，直接应答避免重复推送
        if (!TRADE_SUCCESS.equals(tradeStatus)) {
            log.warn("支付回调非成功状态，忽略：outTradeNo={}, tradeStatus={}", outTradeNo, tradeStatus);
            return buildCallbackResponse(transaction, reservation, false);
        }

        boolean changed = settleSuccess(transaction, reservation, "支付回调");
        return buildCallbackResponse(
                depositTransactionMapper.selectByOutTradeNo(outTradeNo),
                reservationMapper.selectById(reservation.getId()),
                changed);
    }

    /**
     * 支付成功结算：定金流水 SUCCESS + 预约 CONFIRMED。
     *
     * <p>两处都用「当前状态作为 WHERE 条件」的原子更新，因此重复回调/重复支付天然幂等。</p>
     *
     * @return 本次调用是否真正完成了预约确认（用于审计日志与日志输出）
     */
    private boolean settleSuccess(DepositTransaction transaction, Reservation reservation,
                                  String source) {
        // 已支付：幂等返回；同时补一次条件确认，容忍「流水成功但预约未确认」的异常数据。
        if (TX_SUCCESS.equals(transaction.getStatus())) {
            reservationMapper.confirmPaid(reservation.getId());
            return false;
        }
        if (!TX_PENDING.equals(transaction.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "订单状态不允许支付");
        }
        if (!RES_PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "预约状态不允许支付");
        }

        int paid = depositTransactionMapper.markSuccess(transaction.getOutTradeNo());
        if (paid == 0) {
            DepositTransaction latest =
                    depositTransactionMapper.selectByOutTradeNo(transaction.getOutTradeNo());
            if (latest == null || !TX_SUCCESS.equals(latest.getStatus())) {
                throw new BusinessException(ErrorCode.STATUS_INVALID, "订单状态不允许支付");
            }
        }

        int confirmed = reservationMapper.confirmPaid(reservation.getId());
        if (confirmed == 0) {
            Reservation latest = reservationMapper.selectById(reservation.getId());
            if (latest == null || !RES_CONFIRMED.equals(latest.getStatus())) {
                throw new BusinessException(ErrorCode.STATUS_INVALID, "预约状态不允许支付");
            }
            return false;
        }
        writeAuditLog(transaction, reservation, source);
        return true;
    }

    private Map<String, Object> readParams(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回调报文不能为空");
        }
        try {
            return objectMapper.readValue(rawBody,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回调报文格式错误");
        }
    }

    private String requireText(Map<String, Object> params, String key) {
        if (params.get(key) instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, key + " 不能为空");
    }

    private Long requireAmount(Map<String, Object> params) {
        Object value = params.get(FIELD_AMOUNT_CENTS);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                // 落到下面统一报错
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, FIELD_AMOUNT_CENTS + " 不能为空");
    }

    private void writeAuditLog(DepositTransaction transaction, Reservation reservation,
                               String source) {
        AuditLog auditLog = new AuditLog();
        auditLog.setMerchantId(reservation.getMerchantId());
        auditLog.setActorId(reservation.getCustomerId());
        auditLog.setAction(ACTION_PAY_RESERVATION);
        auditLog.setTargetType(TARGET_RESERVATION);
        auditLog.setTargetId(reservation.getId());
        auditLog.setDetail(source + "定金成功，orderNo=" + transaction.getOrderNo()
                + ", amountCents=" + transaction.getAmountCents());
        auditLogMapper.insert(auditLog);
    }

    private PaymentCallbackResponse buildCallbackResponse(DepositTransaction transaction,
                                                          Reservation reservation,
                                                          boolean changed) {
        return PaymentCallbackResponse.builder()
                .outTradeNo(transaction.getOutTradeNo())
                .orderNo(transaction.getOrderNo())
                .reservationId(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .status(reservation.getStatus())
                .amountCents(transaction.getAmountCents())
                .changed(changed)
                .build();
    }

    private PaymentMockPayResponse buildResponse(DepositTransaction transaction,
                                                 Reservation reservation) {
        return PaymentMockPayResponse.builder()
                .outTradeNo(transaction.getOutTradeNo())
                .orderNo(transaction.getOrderNo())
                .reservationId(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .status(reservation.getStatus())
                .amountCents(transaction.getAmountCents())
                .paidAt(reservation.getPaidAt())
                .build();
    }
}
