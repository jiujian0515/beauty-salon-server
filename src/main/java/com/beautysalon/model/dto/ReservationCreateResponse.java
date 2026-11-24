package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建预约返回体
 *
 * <p>普通预约返回 {@code PENDING_PAYMENT} 与模拟支付参数；
 * 疗程预约免定金，直接返回 {@code CONFIRMED}，{@code paymentRequired=false}。</p>
 */
@Data
@Builder
public class ReservationCreateResponse {

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 预约状态 */
    private String status;

    /** 是否疗程预约 */
    private Boolean isCourse;

    /** 定金（分）；疗程预约为 0 */
    private Long depositCents;

    /** 是否需要支付定金 */
    private Boolean paymentRequired;

    /** 支付订单号（仅需支付时有值） */
    private String orderNo;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id */
    private Long resourceId;

    /** 资源名称（STAFF 即美容师名称） */
    private String resourceName;

    /** 时段库存 id */
    private Long slotId;

    /** 模拟支付参数（仅需支付时有值） */
    private Map<String, Object> paymentParams;
}
