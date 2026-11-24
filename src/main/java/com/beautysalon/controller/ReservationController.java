package com.beautysalon.controller;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ReservationCancelResponse;
import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;
import com.beautysalon.model.dto.ReservationResponse;
import com.beautysalon.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端预约接口
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th><th>是否需登录</th></tr>
 *   <tr><td>POST</td><td>/api/v1/reservations</td><td>创建预约</td><td>是（顾客）</td></tr>
 *   <tr><td>GET</td><td>/api/v1/reservations/mine</td><td>我的预约列表</td><td>是（顾客）</td></tr>
 *   <tr><td>POST</td><td>/api/v1/reservations/{id}/cancel</td><td>取消预约</td><td>是（顾客）</td></tr>
 * </table>
 *
 * <p>普通预约返回待支付状态与模拟支付参数；疗程预约免定金，创建后直接确认。</p>
 * <p>取消预约：未支付直接取消；已支付定金模拟退款；需至少提前 24 小时。</p>
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 创建预约（顾客登录后调用） */
    @PostMapping
    public Result<ReservationCreateResponse> create(
            @Valid @RequestBody ReservationCreateRequest request) {
        return Result.ok(reservationService.create(request));
    }

    /**
     * 我的预约列表（顾客登录后调用）
     *
     * <p>示例：GET /api/v1/reservations/mine?page=1&size=10&status=CONFIRMED</p>
     * <ul>
     *   <li>{@code page}：页码，默认 1</li>
     *   <li>{@code size}：每页条数，默认 10，最大 100</li>
     *   <li>{@code status}：可选，预约状态精确筛选（不区分大小写）</li>
     * </ul>
     * <p>只返回当前登录顾客自己的预约，按预约时间倒序。</p>
     */
    @GetMapping("/mine")
    public Result<PageResult<ReservationResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页码必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return Result.ok(reservationService.mine(page, size, status));
    }

    /**
     * 取消预约（顾客登录后调用）
     *
     * <p>示例：POST /api/v1/reservations/12/cancel</p>
     * <ul>
     *   <li>{@code PENDING_PAYMENT}：随时可取消，状态置为 CANCELLED，待支付流水置为 EXPIRED</li>
     *   <li>{@code CONFIRMED}：需至少提前 24 小时，已付定金模拟退款后置为 REFUNDED，否则 CANCELLED</li>
     * </ul>
     * <p>取消成功后释放对应时段库存；重复取消返回当前终态（幂等）。</p>
     */
    @PostMapping("/{id}/cancel")
    public Result<ReservationCancelResponse> cancel(@PathVariable Long id) {
        return Result.ok(reservationService.cancel(id));
    }
}
