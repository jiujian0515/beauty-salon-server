package com.beautysalon.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑角色请求（局部更新：只改传了值的字段）
 */
@Data
public class RoleUpdateRequest {

    /** 新角色名称（不传表示不改） */
    @Size(max = 64, message = "角色名称最长 64 个字符")
    private String name;

    /** 新角色代码（不传表示不改） */
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色代码需为大写字母开头的字母/数字/下划线组合")
    private String code;
}
