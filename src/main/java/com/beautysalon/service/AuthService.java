package com.beautysalon.service;

import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;

/**
 * 认证服务接口（业务层）
 *
 * <p>为什么接口和实现要分开：</p>
 * <ul>
 *   <li>接口定义「做什么」，实现类定义「怎么做」。</li>
 *   <li>Controller 只依赖接口，不关心具体实现，方便以后替换或测试。</li>
 * </ul>
 */
public interface AuthService {

    /**
     * 登录：校验手机号 + 密码，成功则生成登录令牌
     *
     * @param request 登录请求（手机号 + 密码）
     * @return 登录结果（token + 用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 登出：清除当前登录态（token 失效）
     */
    void logout();
}
