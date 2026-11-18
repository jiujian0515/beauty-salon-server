package com.beautysalon.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现（业务核心逻辑都在这里）
 *
 * <p>{@code @Service}：把这个类交给 Spring 管理，Controller 才能注入使用。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 依赖注入的两个对象，通过构造器传入（Spring 启动时自动注入） */
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录流程：
     * 1. 按手机号查用户
     * 2. 校验用户存在 + 密码正确（BCrypt 比对）
     * 3. 校验账号状态
     * 4. 调用 Sa-Token 登录，生成 token
     * 5. 返回 token + 用户信息
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 按手机号查询用户（手机号有唯一索引，最多返回一条）
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );

        // 2. 用户不存在或密码不对，统一提示「手机号或密码错误」（不透露具体是哪个错，防撞库）
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "手机号或密码错误");
        }

        // 3. 校验账号状态（1 启用，0 禁用）
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "账号已被禁用");
        }

        // 4. Sa-Token 登录：以用户 id 作为登录标识，内部生成 token 并维护登录态
        StpUtil.login(user.getId());

        // 5. 组装返回结果
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())   // 当前登录生成的 token
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    /**
     * 登出：Sa-Token 会从请求头 Authorization 里取 token，
     * 找到对应的登录态并清除（token 立即失效）。
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }
}
