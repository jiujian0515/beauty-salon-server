package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增角色请求
 */
@Data
public class RoleCreateRequest {

    /** 角色名称（中文显示用） */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称最长 64 个字符")
    private String name;

    /** 角色代码（大写字母开头，允许字母/数字/下划线，全库唯一） */
    @NotBlank(message = "角色代码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色代码需为大写字母开头的字母/数字/下划线组合")
    private String code;
}
