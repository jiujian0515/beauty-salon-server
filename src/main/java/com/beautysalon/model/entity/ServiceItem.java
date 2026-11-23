package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务项目实体（与数据库 service_item 表一一对应）
 */
@Data
@TableName("service_item")
public class ServiceItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 服务名称 */
    private String name;

    /** 服务分类（商户自定义，不为空） */
    private String category;

    /** 服务描述 */
    private String description;

    /** 服务时长（分钟） */
    private Integer durationMinutes;

    /** 价格（分） */
    private Long priceCents;

    /** 定金（分） */
    private Long depositCents;

    /** 状态：1上架 0下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
