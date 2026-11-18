package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务项目信息返回体
 */
@Data
@Builder
public class ServiceItemResponse {

    /** 服务项目 id */
    private Long id;

    /** 所属商户 id */
    private Long merchantId;

    /** 服务名称 */
    private String name;

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
