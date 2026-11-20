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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 顾客管理实现（后台侧）
 *
 * <p>顾客表全局共享，所以这里所有查询都不带 merchant_id 条件——
 * 与员工/管理员（按商户隔离）不同。</p>
 */
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

    private Customer findCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "顾客不存在");
        }
        return customer;
    }
}
