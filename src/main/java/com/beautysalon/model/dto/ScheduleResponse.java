package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班返回体（含窗口内库存统计）
 */
@Data
@Builder
public class ScheduleResponse {

    /** 排班 id */
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id */
    private Long resourceId;

    /** 资源名称 */
    private String resourceName;

    /** 资源类型：STAFF / ROOM */
    private String resourceType;

    /** 排班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 开始时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 容量 */
    private Integer capacity;

    /** 库存拆分步长（分钟） */
    private Integer slotMinutes;

    /** 状态：1有效 0无效 */
    private Integer status;

    /** 窗口内时段数 */
    private Integer slotCount;

    /** 窗口内总容量 */
    private Integer totalCapacity;

    /** 窗口内已占用数 */
    private Integer usedCount;

    /** 窗口内可售数 */
    private Integer availableCount;

    /** 窗口内锁定数 */
    private Integer lockedCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
