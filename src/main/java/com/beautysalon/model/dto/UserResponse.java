package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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

    /** 角色代码（ADMIN/STAFF/CUSTOMER） */
    private String role;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 所属商户 id（顾客可为空） */
    private Long merchantId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
