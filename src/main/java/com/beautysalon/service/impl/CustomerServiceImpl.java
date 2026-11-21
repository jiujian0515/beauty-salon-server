package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.CustomerMapper;
import com.beautysalon.model.dto.CustomerCreateRequest;
import com.beautysalon.model.dto.CustomerResetPasswordRequest;
import com.beautysalon.model.dto.CustomerResponse;
import com.beautysalon.model.dto.CustomerUpdateRequest;
import com.beautysalon.model.entity.Customer;
import com.beautysalon.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 顾客管理实现（后台侧）
 *
 * <p>顾客表全局共享，所以这里所有查询都不带 merchant_id 条件——
 * 与员工/管理员（按商户隔离）不同。</p>
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerMapper customerMapper, PasswordEncoder passwordEncoder) {
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<CustomerResponse> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 括号必须加：否则 OR 会把条件「拆散」，导致查出不匹配的数据
            wrapper.and(w -> w.like(Customer::getPhone, kw).or().like(Customer::getNickname, kw));
        }

        wrapper.orderByDesc(Customer::getId);

        Page<Customer> result = customerMapper.selectPage(new Page<>(page, size), wrapper);

        List<CustomerResponse> records = result.getRecords().stream()
                .map(CustomerResponse::of)
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public CustomerResponse detail(Long id) {
        return CustomerResponse.of(findCustomer(id));
    }

    @Override
    public CustomerResponse create(CustomerCreateRequest request) {
        Customer customer = createAccount(
                request.getPhone(),
                request.getPassword(),
                request.getNickname(),
                request.getStatus()
        );
        // 重新查一次，把数据库默认值（created_at）带给前端
        return CustomerResponse.of(findCustomer(customer.getId()));
    }

    @Override
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = findCustomer(id);

        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(customer.getPhone())) {
            Long exists = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getPhone, request.getPhone())
                    .ne(Customer::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已被注册");
            }
            customer.setPhone(request.getPhone());
        }

        if (StringUtils.hasText(request.getNickname())) {
            customer.setNickname(request.getNickname());
        }

        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }

        customerMapper.updateById(customer);
        return CustomerResponse.of(customer);
    }

    @Override
    public void delete(Long id) {
        Customer customer = findCustomer(id);

        // 逻辑删除后手机号仍占用唯一索引，先改名释放，保证同号可重新注册
        customerMapper.update(null, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, id)
                .set(Customer::getPhone, customer.getPhone() + "#del" + id));

        customerMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, CustomerResetPasswordRequest request) {
        Customer customer = findCustomer(id);
        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerMapper.updateById(customer);
    }

    @Override
    public Customer createAccount(String phone, String rawPassword, String nickname, Integer status) {
        Long exists = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已被注册");
        }

        Customer customer = new Customer();
        customer.setPhone(phone);
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setNickname(nickname);
        customer.setStatus(status == null ? 1 : status);
        customerMapper.insert(customer);
        return customer;
    }

    /**
     * 微信手机号快捷登录：按手机号查找顾客，不存在则自动创建
     *
     * <p>以手机号作为主账号标识，openid 仅作为微信身份绑定：</p>
     * <ol>
     *   <li>手机号已存在：登录该顾客，并在合适时补绑/更新 openid。</li>
     *   <li>手机号不存在但 openid 已存在：说明同一微信换了手机号，更新手机号。</li>
     *   <li>都不存在：创建无密码顾客账号。</li>
     * </ol>
     */
    @Override
    @Transactional
    public Customer findOrCreateByWechat(String phone, String openid) {
        Customer byPhone = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone));

        if (byPhone != null) {
            bindOpenidIfNecessary(byPhone, openid);
            return byPhone;
        }

        // 同一微信更换手机号：新手机号未被占用，直接更新到原账号
        Customer byOpenid = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getWechatOpenid, openid));
        if (byOpenid != null) {
            byOpenid.setPhone(phone);
            customerMapper.updateById(byOpenid);
            return byOpenid;
        }

        Customer customer = new Customer();
        customer.setPhone(phone);
        customer.setPasswordHash(null);
        customer.setWechatOpenid(openid);
        customer.setNickname("微信用户" + phoneSuffix(phone));
        customer.setStatus(1);

        try {
            customerMapper.insert(customer);
        } catch (DuplicateKeyException e) {
            // 并发首次登录：另一个请求已经创建，重新查询即可
            Customer existing = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getPhone, phone));
            if (existing == null) {
                throw e;
            }
            return existing;
        }
        return customer;
    }

    /**
     * 补绑微信 openid
     *
     * <p>如果 openid 已被其他顾客占用，则保留原绑定，仍允许按手机号登录，
     * 避免「同一个手机号被多个微信账号使用」时把别人的绑定抢走。</p>
     */
    private void bindOpenidIfNecessary(Customer customer, String openid) {
        if (!StringUtils.hasText(openid) || openid.equals(customer.getWechatOpenid())) {
            return;
        }

        Long occupied = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getWechatOpenid, openid));
        if (occupied != null && occupied > 0) {
            log.warn("微信 openid 已绑定其他顾客，本次不更新绑定，顾客 id={}", customer.getId());
            return;
        }

        customer.setWechatOpenid(openid);
        try {
            customerMapper.updateById(customer);
        } catch (DuplicateKeyException e) {
            // 并发绑定冲突：忽略即可，登录仍按手机号完成
            log.warn("微信 openid 绑定并发冲突，顾客 id={}", customer.getId());
        }
    }

    private String phoneSuffix(String phone) {
        return phone.length() > 4 ? phone.substring(phone.length() - 4) : phone;
    }

    private Customer findCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "顾客不存在");
        }
        return customer;
    }
}
