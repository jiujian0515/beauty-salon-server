package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.ReservationCancelResponse;
import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;
import com.beautysalon.model.dto.ReservationResponse;

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

    /**
     * 顾客分页查询自己的预约
     *
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param status 可选，预约状态精确筛选（不区分大小写）
     * @return 按预约时间倒序的分页结果
     */
    PageResult<ReservationResponse> mine(int page, int size, String status);

    /**
     * 顾客取消自己的预约
     *
     * <p>已支付定金的预约走模拟退款并置为 REFUNDED；未支付/免定金预约直接置为 CANCELLED。
     * 两种情况都会释放时段库存并写审计日志。</p>
     *
     * @param id 预约单 id
     * @return 取消结果（含是否退款及金额）
     */
    ReservationCancelResponse cancel(Long id);
}
