package com.beautysalon.service;

import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;

/**
 * 预约服务
 */
public interface ReservationService {

    /**
     * 顾客创建预约
     *
     * <p>普通预约生成待支付预约单与定金流水；疗程预约免定金，直接确认。</p>
     */
    ReservationCreateResponse create(ReservationCreateRequest request);
}
