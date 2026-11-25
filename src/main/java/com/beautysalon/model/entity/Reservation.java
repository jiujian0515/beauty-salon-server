package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约单实体（与数据库 reservation 表一一对应）
 *
 * <p>状态流转：</p>
 * <pre>
 * 普通预约：PENDING_PAYMENT --支付成功--> CONFIRMED --核销--> COMPLETED
 * 疗程预约：创建即 CONFIRMED（免定金）
 * 取消/超时：PENDING_PAYMENT -> CANCELLED / EXPIRED
 * </pre>
 */
@Data
@TableName("reservation")
public class Reservation {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约业务号（唯一） */
    private String reservationNo;

    /** 所属商户 */
    private Long merchantId;

    /** 顾客 id */
    private Long customerId;

    /** 服务项目 id */
    private Long serviceItemId;

    /** 资源 id（技师/床位） */
    private Long resourceId;

    /** 时段库存 id */
    private Long slotInventoryId;

    /** 预约时间（时段日期 + 开始时间） */
    private LocalDateTime appointmentTime;

    /** 定金（分）；疗程预约为 0 */
    private Long depositCents;

    /** 是否疗程预约：1是（免定金） 0否 */
    private Integer isCourse;

    /** 状态：PENDING_PAYMENT/CONFIRMED/COMPLETED/CANCELLED/EXPIRED/REFUNDING/REFUNDED */
    private String status;

    /** 核销码（6 位数字，商户内唯一；历史数据可能为空） */
    private String verifyCode;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 核销时间 */
    private LocalDateTime verifiedAt;

    /** 核销人（后台用户 id） */
    private Long verifiedBy;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
