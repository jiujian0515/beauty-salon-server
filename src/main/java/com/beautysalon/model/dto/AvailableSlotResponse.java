package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 顾客端可售时段返回体
 *
 * <p>只包含真正可预约的时段：资源启用、时段可售、还有余量、
 * 且开始时间满足预约提前量。</p>
 */
@Data
@Builder
public class AvailableSlotResponse {

    /** 时段 id */
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id */
    private Long resourceId;

    /** 资源名称 */
    private String resourceName;

    /** 资源类型：STAFF 技师 / ROOM 床位 */
    private String resourceType;

    /** 时段日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate slotDate;

    /** 开始时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 总容量 */
    private Integer total;

    /** 已占用数 */
    private Integer used;

    /** 剩余可售数 */
    private Integer available;
}
