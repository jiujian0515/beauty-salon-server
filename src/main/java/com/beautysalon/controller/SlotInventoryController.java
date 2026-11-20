package com.beautysalon.controller;

import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.SlotInventoryResponse;
import com.beautysalon.service.SlotInventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 时段库存接口（商户端）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/slots</td><td>分页列表</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/slots/{id}/lock</td><td>锁定时段</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/slots/{id}/unlock</td><td>释放时段</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/admin/slots")
public class SlotInventoryController {

    private final SlotInventoryService slotInventoryService;

    public SlotInventoryController(SlotInventoryService slotInventoryService) {
        this.slotInventoryService = slotInventoryService;
    }

    /**
     * 分页查询时段库存
     *
     * <p>示例：GET /api/v1/admin/slots?page=1&size=10&resourceId=1&startDate=2026-01-01&endDate=2026-01-07&status=1</p>
     */
    @GetMapping
    public Result<PageResult<SlotInventoryResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer status) {
        return Result.ok(slotInventoryService.page(page, size, resourceId, startDate, endDate, status));
    }

    /** 锁定时段（暂停售卖） */
    @PostMapping("/{id}/lock")
    public Result<SlotInventoryResponse> lock(@PathVariable Long id) {
        return Result.ok(slotInventoryService.lock(id));
    }

    /** 释放时段（恢复售卖） */
    @PostMapping("/{id}/unlock")
    public Result<SlotInventoryResponse> unlock(@PathVariable Long id) {
        return Result.ok(slotInventoryService.unlock(id));
    }
}
