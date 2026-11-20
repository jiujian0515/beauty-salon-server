package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 日历视图中的一天
 */
@Data
@Builder
public class CalendarDayResponse {

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** 当天排班数 */
    private int scheduleCount;

    /** 当天时段数 */
    private int slotCount;

    /** 当天总容量 */
    private int totalCapacity;

    /** 当天已占用数 */
    private int usedCount;

    /** 当天可售数 */
    private int availableCount;

    /** 当天锁定数 */
    private int lockedCount;

    /** 当天排班列表 */
    private List<ScheduleResponse> schedules;
}
