package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源信息返回体
 */
@Data
@Builder
public class ResourceResponse {

    /** 资源 id */
    private Long id;

    /** 所属商户 id */
    private Long merchantId;

    /** 类型：STAFF 技师 / ROOM 床位 */
    private String type;

    /** 绑定用户 id（仅 STAFF 有值） */
    private Long userId;

    /** 绑定用户昵称（仅 STAFF 有值） */
    private String userName;

    /** 绑定用户手机号（仅 STAFF 有值） */
    private String userPhone;

    /** 资源名称 */
    private String name;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
