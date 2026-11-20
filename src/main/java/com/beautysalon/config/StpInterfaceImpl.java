package com.beautysalon.config;

import cn.dev33.satoken.stp.StpInterface;
import com.beautysalon.common.LoginIdSupport;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.UserRoleService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色/权限数据源
 *
 * <p>为什么需要这个类？</p>
 * <p>当你在 Controller 上写 {@code @SaCheckRole("ADMIN")} 时，Sa-Token 需要知道
 * 「当前登录用户有哪些角色」。但它不知道角色存在哪里（可能在数据库、可能在 Redis），
 * 所以它要求你实现 {@link StpInterface} 接口，把角色查出来交给它。</p>
 *
 * <p>流程：</p>
 * <pre>
 * 请求 → @SaCheckRole("ADMIN") → Sa-Token 调用 getRoleList(loginId)
 *      → 本类按用户 id 查数据库得到 ["ADMIN"]
 *      → 包含 "ADMIN" ？放行 ： 抛 NotRoleException（被全局异常处理器转成 40301）
 * </pre>
 *
 * <p>注意：loginId 是登录时 {@code StpUtil.login(user.getId())} 传进去的那个值。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final UserRoleService userRoleService;

    public StpInterfaceImpl(UserMapper userMapper, UserRoleService userRoleService) {
        this.userMapper = userMapper;
        this.userRoleService = userRoleService;
    }

    /**
     * 返回该用户的权限码列表
     *
     * <p>本项目用「角色」做粗粒度控制，没做细粒度权限点，所以返回空集合。</p>
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 返回该用户的角色代码列表（来自 user_role 关联表，如 ["ADMIN"]）
     *
     * <p>用户与角色是多对多，所以这里返回的是列表；一个用户可能有多个角色，
     * 只要有任意一个命中 {@code @SaCheckRole} 的值就放行。</p>
     *
     * <p>注意：顾客账号（登录标识 {@code customer:{id}}）不参与后台角色体系，
     * 这里返回空集合，因此顾客无法访问任何 {@code @SaCheckRole} 保护的接口。</p>
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // loginId 传进来是 Object，实际是登录时存的字符串标识（user:1 / customer:1）
        String loginIdStr = String.valueOf(loginId);

        if (LoginIdSupport.isCustomer(loginIdStr)) {
            return Collections.emptyList();
        }

        long userId = LoginIdSupport.parseId(loginIdStr, LoginIdSupport.PREFIX_USER);

        // 用户不存在（含已逻辑删除）→ 返回空集合（@SaCheckRole 会判定为无权限）
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        return userRoleService.roleCodesOf(user.getId());
    }
}
