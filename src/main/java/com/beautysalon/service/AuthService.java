package com.beautysalon.service;

import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.dto.UserInfoResponse;

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

    /**
     * 查询当前登录用户的信息
     *
     * <p>注意：这个方法没有「用户 id」参数。用户身份是从请求头里的 token 解析出来的，
     * 前端无法伪造别人的 id 来查别人的信息（这也是它比「前端传 userId」安全的原因）。</p>
     *
     * @return 当前登录用户的信息
     */
    UserInfoResponse currentUser();
}
