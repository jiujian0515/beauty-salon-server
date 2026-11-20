package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增用户请求（后台账号：管理员 / 员工）
 *
 * <p>顾客不在这里创建——顾客有独立的账号体系，见 {@link CustomerCreateRequest}。</p>
 * <p>角色通过 {@code roleIds} 关联，支持一个用户多个角色（参考若依）。</p>
 */
@Data
public class UserCreateRequest {

    /** 手机号（登录账号，全库唯一） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 初始密码（后端用 BCrypt 加密后入库，绝不存明文） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /** 姓名/昵称 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名最长 64 个字符")
    private String nickname;

    /**
     * 角色 id 列表（用户与角色多对多，至少选择一个）
     *
     * <p>注意：CUSTOMER 是顾客专属角色，后台账号不允许使用。</p>
     */
    @NotEmpty(message = "请至少选择一个角色")
    private List<Long> roleIds;

    /** 状态：1启用 0禁用（不传默认 1 启用） */
    private Integer status;
}
