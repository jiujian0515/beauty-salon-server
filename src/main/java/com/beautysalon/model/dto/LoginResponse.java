package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应结果（登录成功后返回给前端的数据）
 *
 * <p><b>设计约定：登录只返回 token，不返回任何用户信息。</b></p>
 * <ul>
 *   <li>登录接口的职责只有一个：验证身份 + 发放令牌。职责越单一越不容易出错。</li>
 *   <li>用户信息（昵称、角色、商户等）统一由 {@code GET /api/v1/auth/me} 提供，
 *       保证「用户信息只有一个来源」，不会出现登录返回的角色和 /me 返回的角色不一致。</li>
 *   <li>前端拿到 token 后紧接着调一次 /me，即可拿到最新用户信息；
 *       刷新页面时也只需调 /me，不需要额外逻辑（登录流程与恢复登录流程完全一致）。</li>
 * </ul>
 *
 * <p>{@code @Builder}：Lombok 生成建造者模式，方便链式构造对象。</p>
 */
@Data
@Builder
public class LoginResponse {

    /** 登录令牌，前端后续请求放到请求头 Authorization 中 */
    private String token;
}
