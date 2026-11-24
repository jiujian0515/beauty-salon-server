package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 顾客端「我的预约」列表项
 *
 * <p>在预约单字段基础上补充服务项目与资源信息，方便小程序列表直接展示；
 * 只暴露当前顾客自己的预约，商户名称不返回（仅 merchantId）。</p>
 */
@Data
@Builder
public class ReservationResponse {

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 预约状态 */
    private String status;

    /** 是否疗程预约 */
    private Boolean isCourse;

    /** 定金（分）；疗程预约为 0 */
    private Long depositCents;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;

    /** 所属商户 */
    private Long merchantId;

    /** 服务项目 id */
    private Long serviceItemId;

    /** 服务项目名称 */
    private String serviceItemName;

    /** 服务时长（分钟） */
    private Integer serviceItemDurationMinutes;

    /** 服务分类 code（下架或缺省可能为空） */
    private String serviceItemCategory;

    /** 服务价格（分） */
    private Long serviceItemPriceCents;

    /** 资源 id（技师/床位） */
    private Long resourceId;

    /** 资源名称（技师名） */
    private String resourceName;

    /** 资源类型：STAFF 技师 / ROOM 床位 */
    private String resourceType;

    /** 待支付预约对应的模拟支付平台交易号；非待支付或免定金时为 null */
    private String outTradeNo;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    /** 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
