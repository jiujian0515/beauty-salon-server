package com.beautysalon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ReservationAdminResponse;
import com.beautysalon.model.dto.ReservationVerifyRequest;
import com.beautysalon.model.dto.ReservationVerifyResponse;
import com.beautysalon.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 商户端预约核销接口（管理员 / 员工可用）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/reservations</td><td>核销单分页查询</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/reservations/{id}</td><td>核销单详情</td></tr>
 *   <tr><td>POST</td><td>/api/v1/admin/reservations/verify</td><td>按核销码核销预约</td></tr>
 * </table>
 *
 * <p>所有查询自动按当前登录账号所属商户隔离；核销仅允许 CONFIRMED → COMPLETED。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/reservations")
@SaCheckRole(value = {"ADMIN", "STAFF"}, mode = SaMode.OR)
public class AdminReservationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * 核销单分页查询
     *
     * <p>示例：GET /api/v1/admin/reservations?page=1&size=10&status=CONFIRMED&startDate=2026-11-25&endDate=2026-11-25</p>
     * <ul>
     *   <li>{@code status}：可选，预约状态精确筛选（不区分大小写）</li>
     *   <li>{@code verifyCode}：可选，核销码精确筛选</li>
     *   <li>{@code keyword}：可选，模糊匹配预约号 / 核销码 / 顾客手机号 / 顾客昵称</li>
     *   <li>{@code startDate}、{@code endDate}：可选，按预约日期范围筛选（含首尾）</li>
     * </ul>
     */
    @GetMapping
    public Result<PageResult<ReservationAdminResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verifyCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页码必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return Result.ok(reservationService.adminPage(
                page, size, status, verifyCode, keyword, startDate, endDate));
    }

    /** 核销单详情 */
    @GetMapping("/{id}")
    public Result<ReservationAdminResponse> detail(@PathVariable Long id) {
        return Result.ok(reservationService.adminDetail(id));
    }

    /**
     * 按核销码核销预约
     *
     * <p>示例：POST /api/v1/admin/reservations/verify，请求体 {"verifyCode":"123456"}</p>
     * <p>只有 CONFIRMED 状态的预约可以核销，成功后状态变为 COMPLETED。</p>
     */
    @PostMapping("/verify")
    public Result<ReservationVerifyResponse> verify(
            @Valid @RequestBody ReservationVerifyRequest request) {
        return Result.ok(reservationService.verify(request));
    }
}
