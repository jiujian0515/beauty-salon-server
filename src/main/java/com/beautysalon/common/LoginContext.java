package com.beautysalon.common;

import cn.dev33.satoken.stp.StpUtil;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.entity.User;
import org.springframework.stereotype.Component;

/**
 * 登录上下文工具：随时获取「当前是哪个用户 / 属于哪个商户」
 *
 * <p>为什么需要它？因为几乎每个业务方法都要用到「当前用户」和「商户 id」。
 * 如果每个 Service 都自己写 {@code StpUtil.getLoginIdAsLong()} + 查库，代码会重复。</p>
 *
 * <p>用法（在任意 Service 里注入即可）：</p>
 * <pre>
 * long merchantId = loginContext.merchantId();  // 当前商户，用于数据隔离
 * long userId     = loginContext.userId();      // 当前用户 id
 * </pre>
 *
 * <p>⚠️ 注意：只能在「已登录」的请求里调用（SaTokenConfig 已保证 /api/v1/** 需要登录）。</p>
 */
@Component
public class LoginContext {

    private final UserMapper userMapper;

    public LoginContext(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 当前登录标识（字符串，形如 {@code user:1}）
     *
     * <p>本项目有两套账号池（user / customer），所以登录标识带前缀区分。</p>
     */
    public String loginId() {
        return StpUtil.getLoginIdAsString();
    }

    /**
     * 当前登录的后台用户 id（从请求头 token 解析，前端无法伪造）
     *
     * <p>如果当前 token 是顾客登录的（customer:1），这里会直接拒绝——
     * 顾客账号不允许调用后台接口。</p>
     */
    public long userId() {
        String loginId = loginId();
        if (LoginIdSupport.isCustomer(loginId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "当前是顾客账号，请使用后台账号登录");
        }
        return LoginIdSupport.parseId(loginId, LoginIdSupport.PREFIX_USER);
    }

    /**
     * 当前登录用户完整信息（查库，保证是最新的）
     *
     * <p>token 有效不代表账号一定还能用：用户可能已被删除或禁用。
     * 这里主动校验并清除登录态，避免「已禁用账号还能继续访问」。</p>
     */
    public User currentUser() {
        User user = userMapper.selectById(userId());

        if (user == null) {
            StpUtil.logout();
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号不存在，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw new BusinessException(ErrorCode.NO_PERMISSION, "账号已被禁用");
        }
        return user;
    }

    /**
     * 当前登录用户所属商户 id
     *
     * <p>这是多租户数据隔离的关键：所有业务查询都必须带上它，
     * 否则 A 商户的管理员就能查到 B 商户的数据（越权漏洞）。</p>
     */
    public long merchantId() {
        Long merchantId = currentUser().getMerchantId();
        if (merchantId == null) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "当前账号未绑定商户");
        }
        return merchantId;
    }
}
