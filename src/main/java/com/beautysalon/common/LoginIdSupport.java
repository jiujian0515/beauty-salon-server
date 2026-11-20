package com.beautysalon.common;

/**
 * Sa-Token 登录标识工具（区分「后台用户」与「顾客」两套账号池）
 *
 * <p>为什么需要前缀？因为 user 表和 customer 表是两张独立的表，主键都会从 1 开始自增。
 * 如果登录时都写 {@code StpUtil.login(1L)}，那么「员工 id=1」和「顾客 id=1」在
 * Sa-Token 眼里就是同一个登录态，会互相顶掉甚至串号。</p>
 *
 * <p>解决办法：登录标识统一用字符串前缀：</p>
 * <ul>
 *   <li>后台用户（管理员/员工）：{@code user:1}</li>
 *   <li>顾客：{@code customer:1}</li>
 * </ul>
 */
public final class LoginIdSupport {

    /** 后台用户登录标识前缀 */
    public static final String PREFIX_USER = "user:";

    /** 顾客登录标识前缀 */
    public static final String PREFIX_CUSTOMER = "customer:";

    private LoginIdSupport() {
    }

    /** 后台用户登录标识：user:{id} */
    public static String ofUser(Long userId) {
        return PREFIX_USER + userId;
    }

    /** 顾客登录标识：customer:{id} */
    public static String ofCustomer(Long customerId) {
        return PREFIX_CUSTOMER + customerId;
    }

    /** 当前登录标识是否属于顾客账号 */
    public static boolean isCustomer(String loginId) {
        return loginId != null && loginId.startsWith(PREFIX_CUSTOMER);
    }

    /**
     * 从登录标识里解析出数据库主键
     *
     * @param loginId 登录标识（如 user:12）
     * @param prefix  期望的前缀
     * @return 主键 id
     */
    public static long parseId(String loginId, String prefix) {
        if (loginId == null || !loginId.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "登录状态无效，请重新登录");
        }
        try {
            return Long.parseLong(loginId.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "登录状态无效，请重新登录");
        }
    }
}
