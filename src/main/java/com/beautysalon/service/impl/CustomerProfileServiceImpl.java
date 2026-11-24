package com.beautysalon.service.impl;

import com.beautysalon.common.CustomerContext;
import com.beautysalon.mapper.ReservationMapper;
import com.beautysalon.model.dto.CustomerProfileResponse;
import com.beautysalon.model.dto.ReservationStatsResponse;
import com.beautysalon.model.entity.Customer;
import com.beautysalon.service.CustomerProfileService;
import org.springframework.stereotype.Service;

/**
 * 顾客端个人信息实现
 *
 * <p>资料来自 customer 表（经 {@link CustomerContext} 校验登录态与账号状态），
 * 统计来自 reservation 表的一次聚合查询，避免个人中心页面多次请求。</p>
 */
@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerContext customerContext;
    private final ReservationMapper reservationMapper;

    public CustomerProfileServiceImpl(CustomerContext customerContext,
                                      ReservationMapper reservationMapper) {
        this.customerContext = customerContext;
        this.reservationMapper = reservationMapper;
    }

    @Override
    public CustomerProfileResponse currentProfile() {
        Customer customer = customerContext.currentCustomer();

        ReservationStatsResponse stats = reservationMapper.countStatsByCustomerId(customer.getId());
        if (stats == null) {
            // 理论上聚合查询总会返回一行，这里兜底避免 NPE
            stats = new ReservationStatsResponse();
        }

        return CustomerProfileResponse.builder()
                .customerId(customer.getId())
                .phone(customer.getPhone())
                .nickname(customer.getNickname())
                .status(customer.getStatus())
                .reservationStats(stats)
                .build();
    }
}
