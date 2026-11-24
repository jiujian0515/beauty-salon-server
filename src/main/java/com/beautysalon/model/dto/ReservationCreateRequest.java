package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建预约请求
 *
 * <p>预约时间由时段库存决定，不需要前端额外传 appointmentTime。</p>
 */
@Data
public class ReservationCreateRequest {

    /** 服务项目 id */
    @NotNull(message = "不能为空")
    private Long serviceItemId;

    /** 时段库存 id */
    @NotNull(message = "不能为空")
    private Long slotId;

    /**
     * 是否疗程预约
     *
     * <p>为 true 时免付定金，创建后直接确认；不传或 false 按普通预约处理。</p>
     */
    private Boolean isCourse;
}
