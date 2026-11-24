package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 取消预约返回体
 */
@Data
@Builder
public class ReservationCancelResponse {

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 取消后的状态：CANCELLED / REFUNDED */
    private String status;

    /** 是否已退款 */
    private Boolean refunded;

    /** 退款金额（分）；未退款为 0 */
    private Long refundAmountCents;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;
}
