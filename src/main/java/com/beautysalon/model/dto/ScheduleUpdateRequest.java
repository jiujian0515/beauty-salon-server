package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 编辑排班请求（局部更新）
 *
 * <p>资源不允许更换；已有预约历史时只允许改容量和状态。</p>
 */
@Data
public class ScheduleUpdateRequest {

    /** 排班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 开始时间，格式 HH:mm */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 结束时间，格式 HH:mm */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 容量 */
    @Min(value = 1, message = "容量至少为 1")
    private Integer capacity;

    /** 库存拆分步长（分钟） */
    @Min(value = 5, message = "拆分步长至少 5 分钟")
    @Max(value = 480, message = "拆分步长最多 480 分钟")
    private Integer slotMinutes;

    /** 状态：1有效 0无效 */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
