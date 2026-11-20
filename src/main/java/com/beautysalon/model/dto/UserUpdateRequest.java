package com.beautysalon.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑用户请求（局部更新：只改传了值的字段）
 */
@Data
public class UserUpdateRequest {

    /** 新手机号（不传表示不改） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 新姓名（不传表示不改） */
    @Size(max = 64, message = "姓名最长 64 个字符")
    private String nickname;

    /**
     * 新角色 id 列表（不传表示不改；至少保留一个角色）
     *
     * <p>管理员账号的角色不允许修改。</p>
     */
    private List<Long> roleIds;

    /** 状态：1启用 0禁用（不传表示不改） */
    private Integer status;
}
