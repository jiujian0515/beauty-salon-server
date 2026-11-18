package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应结果（登录成功后返回给前端的数据）
 *
 * <p>{@code @Builder}：Lombok 生成建造者模式，方便链式构造对象。</p>
 */
@Data
@Builder
public class LoginResponse {

    /** 登录令牌，前端后续请求放到请求头 Authorization 中 */
    private String token;

    /** 用户 id */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 角色：CUSTOMER/STAFF/ADMIN */
    private String role;
}
