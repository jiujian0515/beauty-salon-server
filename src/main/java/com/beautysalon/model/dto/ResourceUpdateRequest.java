package com.beautysalon.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑资源请求（局部更新：只改传了值的字段）
 *
 * <p>资源类型创建后不允许修改；传相同值会被忽略，传不同值报参数错误。</p>
 */
@Data
public class ResourceUpdateRequest {

    /** 资源类型（只允许传当前类型，用于前端整表单提交） */
    private String type;

    /** 新资源名称（不传表示不改） */
    @Size(max = 64, message = "资源名称最长 64 个字符")
    private String name;

    /** 新绑定用户 id（仅 STAFF 可改；不传表示不改） */
    private Long userId;

    /** 状态：1启用 0禁用（不传表示不改） */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
