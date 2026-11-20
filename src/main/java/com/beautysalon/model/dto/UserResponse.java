package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息返回体
 *
 * <p>⚠️ 注意这里没有 passwordHash 字段——密码哈希永远不能返回给前端。</p>
 */
@Data
@Builder
public class UserResponse {

    /** 用户 id */
    private Long id;

    /** 手机号（登录账号） */
    private String phone;

    /** 姓名/昵称 */
    private String nickname;

    /** 所属商户 id（顾客可为空） */
    private Long merchantId;

    /** 角色列表（用户与角色多对多，参考若依） */
    private List<RoleResponse> roles;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
