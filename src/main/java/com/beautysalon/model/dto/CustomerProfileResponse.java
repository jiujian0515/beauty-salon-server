package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 顾客端「个人信息」聚合响应（GET /api/v1/customer/profile）
 *
 * <p>在 {@link CustomerInfoResponse} 的基础资料之上，附带当前顾客的预约统计，
 * 供个人中心页面一次性渲染资料卡片与预约数据。</p>
 */
@Data
@Builder
public class CustomerProfileResponse {

    /** 顾客 id */
    private Long customerId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 预约统计：累计成功、待服务、待支付 */
    private ReservationStatsResponse reservationStats;
}
