package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色信息返回体
 */
@Data
@Builder
public class RoleResponse {

    /** 角色 id */
    private Long id;

    /** 角色名称（中文显示用） */
    private String name;

    /** 角色代码（如 ADMIN/STAFF） */
    private String code;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 实体 → 返回 DTO */
    public static RoleResponse of(com.beautysalon.model.entity.Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
