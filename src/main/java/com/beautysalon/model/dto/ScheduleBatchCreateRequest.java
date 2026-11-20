package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 批量新增排班请求
 *
 * <p>按日期范围 + 星期几生成多条排班；{@code weekdays} 为空表示每天都生成。</p>
 */
@Data
public class ScheduleBatchCreateRequest {

    /** 资源 id */
    @NotNull(message = "资源不能为空")
    private Long resourceId;

    /** 开始日期 */
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 结束日期（含当天） */
    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 星期几：1=周一 ... 7=周日；为空表示每天 */
    @Size(max = 7, message = "星期最多 7 个")
    private List<@Min(value = 1, message = "星期只能是 1-7")
                 @Max(value = 7, message = "星期只能是 1-7") Integer> weekdays;

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
