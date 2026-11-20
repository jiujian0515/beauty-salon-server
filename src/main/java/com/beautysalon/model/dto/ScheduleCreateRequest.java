package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 新增排班请求
 *
 * <p>保存后会按 {@code slotMinutes} 把时间窗口拆成时段库存。</p>
 */
@Data
public class ScheduleCreateRequest {

    /** 资源 id（STAFF 技师 / ROOM 床位） */
    @NotNull(message = "资源不能为空")
    private Long resourceId;

    /** 排班日期 */
    @NotNull(message = "排班日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 开始时间，格式 HH:mm */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 结束时间，格式 HH:mm */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 容量，默认 1 */
    @Min(value = 1, message = "容量至少为 1")
    private Integer capacity;

    /** 库存拆分步长（分钟），默认 60 */
    @Min(value = 5, message = "拆分步长至少 5 分钟")
    @Max(value = 480, message = "拆分步长最多 480 分钟")
    private Integer slotMinutes;

    /** 状态：1有效 0无效，默认 1 */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
