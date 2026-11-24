package com.beautysalon.service;

import com.beautysalon.model.dto.CustomerProfileResponse;

/**
 * 顾客端个人信息服务
 */
public interface CustomerProfileService {

    /**
     * 当前登录顾客的个人信息与预约统计
     *
     * <p>统计口径：累计成功预约（COMPLETED）、待服务（CONFIRMED）、待支付（PENDING_PAYMENT）。</p>
     */
    CustomerProfileResponse currentProfile();
}
