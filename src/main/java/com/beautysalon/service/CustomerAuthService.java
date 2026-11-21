package com.beautysalon.service;

import com.beautysalon.model.dto.CustomerInfoResponse;
import com.beautysalon.model.dto.CustomerRegisterRequest;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.dto.WechatPhoneLoginRequest;

/**
 * 顾客端认证服务（自助注册 / 登录 / 登出 / 当前顾客信息）
 *
 * <p>与后台的 {@link AuthService} 完全分开：账号池不同（customer 表 vs user 表），
 * 登录标识也不同（{@code customer:{id}} vs {@code user:{id}}）。</p>
 */
public interface CustomerAuthService {

    /** 顾客自助注册（注册成功后直接返回 token，等于自动登录） */
    LoginResponse register(CustomerRegisterRequest request);

    /** 顾客手机号 + 密码登录 */
    LoginResponse login(LoginRequest request);

    /**
     * 微信手机号快速登录
     *
     * <p>用 wx.login 的 code 换 openid、用 getPhoneNumber 的 code 换手机号，
     * 手机号未注册时自动创建顾客账号并直接登录。</p>
     */
    LoginResponse wechatLogin(WechatPhoneLoginRequest request);

    /** 顾客登出 */
    void logout();

    /** 当前登录顾客信息 */
    CustomerInfoResponse currentCustomer();
}
