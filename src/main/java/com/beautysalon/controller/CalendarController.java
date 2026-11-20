package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.CalendarDayResponse;
import com.beautysalon.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班日历接口（商户端）
 *
 * <p>返回区间内每一天的排班与库存汇总，没有排班的日期也会返回（用于日历占位）。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/calendar")
public class CalendarController {

    private final ScheduleService scheduleService;

    public CalendarController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 日历汇总
     *
     * <p>示例：GET /api/v1/admin/calendar?startDate=2026-01-01&endDate=2026-01-07&resourceId=1</p>
     * <p>不传日期时默认返回今天起 7 天；一次最多查询 62 天。</p>
     */
    @GetMapping
    public Result<List<CalendarDayResponse>> calendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long resourceId) {
        return Result.ok(scheduleService.calendar(startDate, endDate, resourceId));
    }
}
