package com.beautysalon.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑服务项目请求（局部更新：只改传了值的字段）
 */
@Data
public class ServiceItemUpdateRequest {

    /** 新服务名称（不传表示不改） */
    @Size(max = 128, message = "服务名称最长 128 个字符")
    private String name;

    /** 新服务分类（不传表示不改；传了就不能为空） */
    @Size(max = 64, message = "服务分类最长 64 个字符")
    private String category;

    /** 新服务描述（不传表示不改） */
    @Size(max = 512, message = "服务描述最长 512 个字符")
    private String description;

    /** 新服务时长（不传表示不改） */
    @Min(value = 1, message = "服务时长至少 1 分钟")
    private Integer durationMinutes;

    /** 新价格（不传表示不改） */
    @Min(value = 0, message = "价格不能为负数")
    private Long priceCents;

    /** 新定金（不传表示不改） */
    @Min(value = 0, message = "定金不能为负数")
    private Long depositCents;

    /** 状态：1上架 0下架（不传表示不改） */
    private Integer status;
}
