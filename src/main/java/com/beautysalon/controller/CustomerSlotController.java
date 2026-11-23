package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.AvailableSlotResponse;
import com.beautysalon.service.SlotInventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 顾客端可售时段接口（公开，无需登录）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/slots</td><td>按日期查询可预约时段</td></tr>
 * </table>
 *
 * <p>服务项目与资源没有绑定关系，所以查询参数不需要 serviceId；
 * 可售时段只由「日期 + 资源（+ 商户）」决定。</p>
 */
@RestController
@RequestMapping("/api/v1/slots")
public class CustomerSlotController {

    private final SlotInventoryService slotInventoryService;

    public CustomerSlotController(SlotInventoryService slotInventoryService) {
        this.slotInventoryService = slotInventoryService;
    }

    /**
     * 查询某天可预约的时段
     *
     * <p>示例：GET /api/v1/slots?date=2026-11-25&resourceId=1&merchantId=1</p>
     * <ul>
     *   <li>{@code date}：必填，查询日期（yyyy-MM-dd）</li>
     *   <li>{@code resourceId}：可选，指定技师/房间</li>
     *   <li>{@code merchantId}：可选，不传 resourceId 时限定商户</li>
     * </ul>
     * <p>只返回资源启用、时段可售、还有余量且满足预约提前量的时段。</p>
     */
    @GetMapping
    public Result<List<AvailableSlotResponse>> available(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long merchantId) {
        return Result.ok(slotInventoryService.availableSlots(date, resourceId, merchantId));
    }
}
