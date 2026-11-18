package com.beautysalon.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增服务项目请求
 */
@Data
public class ServiceItemCreateRequest {

    /** 服务名称 */
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128, message = "服务名称最长 128 个字符")
    private String name;

    /** 服务描述 */
    @Size(max = 512, message = "服务描述最长 512 个字符")
    private String description;

    /** 服务时长（分钟） */
    @NotNull(message = "服务时长不能为空")
    @Min(value = 1, message = "服务时长至少 1 分钟")
    private Integer durationMinutes;

    /** 价格（分） */
    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Long priceCents;

    /** 定金（分） */
    @NotNull(message = "定金不能为空")
    @Min(value = 0, message = "定金不能为负数")
    private Long depositCents;

    /** 状态：1上架 0下架（不传默认 1 上架） */
    private Integer status;
}
