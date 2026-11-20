package com.beautysalon.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.LoginIdSupport;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.dto.UserInfoResponse;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.AuthService;
import com.beautysalon.service.UserRoleService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private final LoginContext loginContext;
    private final UserRoleService userRoleService;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           LoginContext loginContext,
                           UserRoleService userRoleService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginContext = loginContext;
        this.userRoleService = userRoleService;
    }

    /**
     * 登录流程：
     * 1. 按手机号查用户
     * 2. 校验用户存在 + 密码正确（BCrypt 比对）
     * 3. 校验账号状态
     * 4. 调用 Sa-Token 登录，生成 token
     * 5. 只返回 token（用户信息请调 GET /api/v1/auth/me）
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

        // 4. Sa-Token 登录：登录标识用 "user:{id}" 前缀，
        //    与顾客端的 "customer:{id}" 区分，避免两张表主键相同导致串号
        StpUtil.login(LoginIdSupport.ofUser(user.getId()));

        // 5. 只返回 token：用户信息由 /api/v1/auth/me 统一提供，避免两处数据不一致
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())   // 当前登录生成的 token
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

    /**
     * 查询当前登录用户信息
     *
     * <p>关键点：用户身份来自 token，不来自前端参数。</p>
     * <ol>
     *   <li>{@code StpUtil.getLoginIdAsString()}：从当前请求的 token 里取出登录时存进去的标识
     *       （后台账号是 {@code user:{id}}，顾客是 {@code customer:{id}}）。</li>
     *   <li>再用这个 id 回数据库查最新信息——这样昵称、角色被后台改过，前端拿到的也是最新的。</li>
     * </ol>
     *
     * <p>为什么还要再查库校验？因为 token 有效 ≠ 账号一定还能用：
     * 用户可能已被删除、被禁用。这时要主动清掉登录态，让前端跳登录页。</p>
     */
    @Override
    public UserInfoResponse currentUser() {
        // 从 token 解析当前用户 id，并校验「账号仍存在且未被禁用」
        // 这段通用逻辑已抽到 LoginContext，多处复用
        User user = loginContext.currentUser();

        // 角色来自 user_role 关联表（多对多），一次查出全部角色代码
        List<String> roles = userRoleService.roleCodesOf(user.getId());

        return UserInfoResponse.builder()
                .userId(user.getId())
                .merchantId(user.getMerchantId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .roles(roles)
                // 主角色：取第一个，兼容只判断单个角色的旧前端逻辑
                .role(roles.isEmpty() ? null : roles.get(0))
                .status(user.getStatus())
                .build();
    }
}
