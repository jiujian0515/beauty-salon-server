package com.beautysalon.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增资源请求
 *
 * <p>STAFF（技师）必须传 {@code userId}，名称不传时默认取用户昵称；
 * ROOM（床位）必须传名称，且不能传 {@code userId}。</p>
 */
@Data
public class ResourceCreateRequest {

    /** 资源类型：STAFF 技师 / ROOM 床位 */
    @NotBlank(message = "资源类型不能为空")
    private String type;

    /** 资源名称（STAFF 可不传，默认取绑定用户昵称） */
    @Size(max = 64, message = "资源名称最长 64 个字符")
    private String name;

    /** 绑定用户 id（STAFF 必填，ROOM 不允许） */
    private Long userId;

    /** 状态：1启用 0禁用（不传默认 1 启用） */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
