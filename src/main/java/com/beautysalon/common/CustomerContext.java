package com.beautysalon.common;

import cn.dev33.satoken.stp.StpUtil;
import com.beautysalon.mapper.CustomerMapper;
import com.beautysalon.model.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * 顾客登录上下文（顾客端的「当前登录人」）
 *
 * <p>和 {@link LoginContext} 的区别：</p>
 * <ul>
 *   <li>{@code LoginContext} 服务后台（管理员/员工），登录标识是 {@code user:{id}}</li>
 *   <li>{@code CustomerContext} 服务顾客端，登录标识是 {@code customer:{id}}</li>
 * </ul>
 *
 * <p>为什么不合二为一？因为两套账号池完全独立：同一个 token 不可能既是员工又是顾客，
 * 分开写能避免「员工调顾客接口」「顾客调后台接口」这类越权问题。</p>
 */
@Component
public class CustomerContext {

    private final CustomerMapper customerMapper;

    public CustomerContext(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    /** 当前登录顾客 id（从 token 解析，前端无法伪造） */
    public long customerId() {
        return LoginIdSupport.parseId(StpUtil.getLoginIdAsString(), LoginIdSupport.PREFIX_CUSTOMER);
    }

    /**
     * 当前登录顾客完整信息（查库，保证是最新的）
     *
     * <p>token 有效 ≠ 账号还能用：顾客可能已被后台删除或禁用，
     * 这里主动校验并清除登录态。</p>
     */
    public Customer currentCustomer() {
        Customer customer = customerMapper.selectById(customerId());

        if (customer == null) {
            StpUtil.logout();
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号不存在，请重新登录");
        }
        if (customer.getStatus() == null || customer.getStatus() != 1) {
            StpUtil.logout();
            throw new BusinessException(ErrorCode.NO_PERMISSION, "账号已被禁用");
        }
        return customer;
    }
}
