package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息（GET /api/v1/auth/me 的返回体）
 *
 * <p>为什么不让接口直接返回 {@link com.beautysalon.model.entity.User} 实体？</p>
 * <ul>
 *   <li>实体会带上 passwordHash（密码哈希），绝不能返回给前端；</li>
 *   <li>实体是「数据库结构」的映射，接口返回是「给前端的契约」，两者要解耦——
 *       以后改表结构不至于直接把前端接口改崩。</li>
 * </ul>
 * 这就是常说的「实体（Entity）不要直接当接口返回值（VO/DTO）」。
 */
@Data
@Builder
public class UserInfoResponse {

    /** 用户 id */
    private Long userId;

    /** 所属商户 id（顾客可能为空） */
    private Long merchantId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 角色代码列表（如 ["ADMIN"]，前端据此控制菜单和按钮显隐） */
    private List<String> roles;

    /**
     * 主角色代码（取 roles 的第一个，兼容只判断单个角色的前端逻辑）
     *
     * <p>完整角色请使用 {@link #roles}。</p>
     */
    private String role;

    /** 状态：1启用 0禁用 */
    private Integer status;
}
