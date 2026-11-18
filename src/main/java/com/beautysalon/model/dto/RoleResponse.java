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

    /** 角色代码（如 CUSTOMER/STAFF/ADMIN） */
    private String code;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
