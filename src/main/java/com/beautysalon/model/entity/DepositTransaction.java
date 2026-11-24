package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定金流水实体（与数据库 deposit_transaction 表一一对应）
 *
 * <p>只有需要支付定金的普通预约才会生成流水；疗程预约免定金，不生成流水。</p>
 */
@Data
@TableName("deposit_transaction")
public class DepositTransaction {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 预约单 id */
    private Long reservationId;

    /** 订单号（唯一） */
    private String orderNo;

    /** 平台交易号（唯一，支付回调幂等键） */
    private String outTradeNo;

    /** 支付渠道 */
    private String channel;

    /** 金额（分） */
    private Long amountCents;

    /** 状态：PENDING/SUCCESS/FAILED/EXPIRED/REFUNDED */
    private String status;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 退款时间 */
    private LocalDateTime refundedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
