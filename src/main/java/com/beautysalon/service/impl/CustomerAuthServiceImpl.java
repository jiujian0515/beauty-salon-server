package com.beautysalon.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.CustomerContext;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginIdSupport;
import com.beautysalon.mapper.CustomerMapper;
import com.beautysalon.model.dto.CustomerInfoResponse;
import com.beautysalon.model.dto.CustomerRegisterRequest;
import com.beautysalon.model.dto.LoginRequest;
import com.beautysalon.model.dto.LoginResponse;
import com.beautysalon.model.entity.Customer;
import com.beautysalon.service.CustomerAuthService;
import com.beautysalon.service.CustomerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 顾客端认证实现
 *
 * <p>关键点：登录标识用 {@code customer:{id}} 前缀，与后台的 {@code user:{id}} 隔离，
 * 这样即使顾客 id 和员工 id 都是 1，两边的登录态也互不影响。</p>
 */
@Service
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final CustomerContext customerContext;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthServiceImpl(CustomerMapper customerMapper,
                                   CustomerService customerService,
                                   CustomerContext customerContext,
                                   PasswordEncoder passwordEncoder) {
        this.customerMapper = customerMapper;
        this.customerService = customerService;
        this.customerContext = customerContext;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 顾客自助注册
     *
     * <p>流程：校验手机号未被注册 → 加密密码入库 → 直接登录并返回 token
     * （前端拿到 token 就能立刻进入首页，不用再让用户手动登录一次）。</p>
     */
    @Override
    public LoginResponse register(CustomerRegisterRequest request) {
        Customer customer = customerService.createAccount(
                request.getPhone(),
                request.getPassword(),
                request.getNickname(),
                1
        );

        StpUtil.login(LoginIdSupport.ofCustomer(customer.getId()));
        return LoginResponse.builder().token(StpUtil.getTokenValue()).build();
    }

    /**
     * 顾客登录
     *
     * <p>注意：这里只查 customer 表，不会命中后台账号；
     * 后台账号请走 {@code POST /api/v1/auth/login}。</p>
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, request.getPhone())
        );

        // 顾客不存在或密码不对，统一提示（不透露具体是哪个错，防撞库）
        if (customer == null || !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "手机号或密码错误");
        }

        if (customer.getStatus() == null || customer.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "账号已被禁用");
        }

        StpUtil.login(LoginIdSupport.ofCustomer(customer.getId()));
        return LoginResponse.builder().token(StpUtil.getTokenValue()).build();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public CustomerInfoResponse currentCustomer() {
        Customer customer = customerContext.currentCustomer();
        return CustomerInfoResponse.builder()
                .customerId(customer.getId())
                .phone(customer.getPhone())
                .nickname(customer.getNickname())
                .status(customer.getStatus())
                .build();
    }
}
