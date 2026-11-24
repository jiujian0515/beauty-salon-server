package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体（只追加，不更新）
 */
@Data
@TableName("audit_log")
public class AuditLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 操作人 */
    private Long actorId;

    /** 动作（如 PAY_RESERVATION） */
    private String action;

    /** 目标类型 */
    private String targetType;

    /** 目标 id */
    private Long targetId;

    /** 详情 */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
