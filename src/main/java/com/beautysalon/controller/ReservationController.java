package com.beautysalon.controller;

import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;
import com.beautysalon.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端预约接口
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th><th>是否需登录</th></tr>
 *   <tr><td>POST</td><td>/api/v1/reservations</td><td>创建预约</td><td>是（顾客）</td></tr>
 * </table>
 *
 * <p>普通预约返回待支付状态与模拟支付参数；疗程预约免定金，创建后直接确认。</p>
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

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
}
