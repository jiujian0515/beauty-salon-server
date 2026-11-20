package com.beautysalon.controller;

import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ScheduleBatchCreateRequest;
import com.beautysalon.model.dto.ScheduleBatchResponse;
import com.beautysalon.model.dto.ScheduleCreateRequest;
import com.beautysalon.model.dto.ScheduleResponse;
import com.beautysalon.model.dto.ScheduleUpdateRequest;
import com.beautysalon.model.dto.SlotInventoryResponse;
import com.beautysalon.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班管理接口（商户端）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/schedules</td><td>分页列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/schedules/{id}</td><td>详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/schedules</td><td>新增（自动生成时段库存）</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/schedules/batch</td><td>批量新增</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/admin/schedules/{id}</td><td>编辑</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/admin/schedules/{id}</td><td>删除</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/schedules/{id}/slots</td><td>窗口内时段库存</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 分页查询排班
     *
     * <p>示例：GET /api/v1/admin/schedules?page=1&size=10&resourceId=1&startDate=2026-01-01&endDate=2026-01-31</p>
     */
    @GetMapping
    public Result<PageResult<ScheduleResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer status) {
        return Result.ok(scheduleService.page(page, size, resourceId, startDate, endDate, status));
    }

    /** 排班详情 */
    @GetMapping("/{id}")
    public Result<ScheduleResponse> detail(@PathVariable Long id) {
        return Result.ok(scheduleService.detail(id));
    }

    /** 新增排班，同时按拆分步长生成时段库存 */
    @PostMapping
    public Result<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest request) {
        return Result.ok(scheduleService.create(request));
    }

    /** 批量新增排班，可按星期过滤，冲突日期会被跳过并返回原因 */
    @PostMapping("/batch")
    public Result<ScheduleBatchResponse> batchCreate(
            @Valid @RequestBody ScheduleBatchCreateRequest request) {
        return Result.ok(scheduleService.batchCreate(request));
    }

    /** 编辑排班（局部更新） */
    @PutMapping("/{id}")
    public Result<ScheduleResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody ScheduleUpdateRequest request) {
        return Result.ok(scheduleService.update(id, request));
    }

    /** 删除排班，已有预约占用时禁止 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.ok();
    }

    /** 查询该排班窗口内的时段库存 */
    @GetMapping("/{id}/slots")
    public Result<List<SlotInventoryResponse>> slots(@PathVariable Long id) {
        return Result.ok(scheduleService.slots(id));
    }
}
