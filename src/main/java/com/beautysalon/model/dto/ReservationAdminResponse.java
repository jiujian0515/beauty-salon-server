package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户端预约单列表 / 详情项
 *
 * <p>在预约单字段基础上补充顾客、服务项目与资源信息，供商户后台核销时展示。</p>
 */
@Data
@Builder
public class ReservationAdminResponse {

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 核销码（6 位数字，商户内唯一） */
    private String verifyCode;

    /** 预约状态 */
    private String status;

    /** 是否疗程预约 */
    private Boolean isCourse;

    /** 定金（分）；疗程预约为 0 */
    private Long depositCents;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;

    /** 顾客 id */
    private Long customerId;

    /** 顾客昵称 */
    private String customerNickname;

    /** 顾客手机号 */
    private String customerPhone;

    /** 服务项目 id */
    private Long serviceItemId;

    /** 服务项目名称 */
    private String serviceItemName;

    /** 服务时长（分钟） */
    private Integer serviceItemDurationMinutes;

    /** 服务分类 code */
    private String serviceItemCategory;

    /** 服务价格（分） */
    private Long serviceItemPriceCents;

    /** 资源 id（技师/床位） */
    private Long resourceId;

    /** 资源名称（技师名） */
    private String resourceName;

    /** 资源类型：STAFF 技师 / ROOM 床位 */
    private String resourceType;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifiedAt;

    /** 核销人（后台用户 id） */
    private Long verifiedBy;

    /** 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
