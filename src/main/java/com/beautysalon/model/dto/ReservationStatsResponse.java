package com.beautysalon.model.dto;

import lombok.Data;

/**
 * 顾客预约统计（个人信息接口中的 reservationStats）
 *
 * <p>口径：</p>
 * <ul>
 *   <li>{@code completedCount}：累计成功预约，即状态为 COMPLETED 的已完成预约</li>
 *   <li>{@code pendingServiceCount}：待服务，即状态为 CONFIRMED 的已确认预约</li>
 *   <li>{@code pendingPaymentCount}：待支付，即状态为 PENDING_PAYMENT 的预约</li>
 * </ul>
 *
 * <p>使用基本类型 long，无记录时由 SQL 的 COUNT 直接返回 0，前端无需处理 null。</p>
 */
@Data
public class ReservationStatsResponse {

    /** 累计成功预约（已完成 COMPLETED） */
    private long completedCount;

    /** 待服务（已确认 CONFIRMED） */
    private long pendingServiceCount;

    /** 待支付（PENDING_PAYMENT） */
    private long pendingPaymentCount;
}
