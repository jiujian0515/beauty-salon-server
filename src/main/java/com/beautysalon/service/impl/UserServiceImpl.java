package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.RoleMapper;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.UserCreateRequest;
import com.beautysalon.model.dto.UserResetPasswordRequest;
import com.beautysalon.model.dto.UserResponse;
import com.beautysalon.model.dto.UserUpdateRequest;
import com.beautysalon.model.entity.Role;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 用户账号管理实现（管理员 / 员工 / 顾客统一管理）
 *
 * <p>数据隔离：管理员/员工按 {@code merchant_id = 当前商户} 隔离；
 * 顾客全局共享（{@code merchant_id} 为空），所有商户可见可管理。</p>
 */
@Service
public class UserServiceImpl implements UserService {

    /** 顾客角色代码：顾客全局共享，不绑定商户 */
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginContext loginContext;

    public UserServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder,
                           LoginContext loginContext) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginContext = loginContext;
    }

    @Override
    public PageResult<UserResponse> page(int page, int size, String keyword, String role) {
        long merchantId = loginContext.merchantId();

        // 1. 基础条件：本商户用户（管理员/员工）+ 全局顾客（merchant_id 为空）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(User::getMerchantId, merchantId)
                .or().isNull(User::getMerchantId));

        // 2. 角色筛选（可选）
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role.trim());
        }

        // 3. 姓名/手机号 模糊搜索，用 and(...) 包起来保证是
        //    (merchant_id=? AND (phone LIKE ? OR nickname LIKE ?))
        //    避免 OR 破坏前面的隔离条件
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, kw).or().like(User::getNickname, kw));
        }

        // 4. 按 id 倒序（最新创建的在前）
        wrapper.orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        List<UserResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public UserResponse detail(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        long merchantId = loginContext.merchantId();

        // 1. 校验角色代码必须存在于角色字典
        validateRoleCode(request.getRole());

        // 2. 手机号查重（user 表手机号是唯一索引，登录账号不能重复）
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已被使用");
        }

        // 3. 组装实体：顾客全局共享（不绑定商户），管理员/员工绑定当前商户
        User user = new User();
        user.setMerchantId(ROLE_CUSTOMER.equals(request.getRole()) ? null : merchantId);
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        userMapper.insert(user);
        return toResponse(user);
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);

        // 1. 改手机号前要查重（排除自己）
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            Long exists = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
            );
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }

        // 2. 局部更新：只改传了值的字段
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getRole())) {
            validateRoleCode(request.getRole());
            user.setRole(request.getRole());
            // 角色决定是否绑定商户：顾客全局共享，其余绑定当前商户
            user.setMerchantId(ROLE_CUSTOMER.equals(request.getRole()) ? null : loginContext.merchantId());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    public void delete(Long id) {
        User user = findUser(id);

        // 先「释放手机号」：逻辑删除后记录还在，唯一索引仍占着这个号，
        // 追加 #del{id} 后缀腾出号码，方便同一手机号重新建档。
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getPhone, user.getPhone() + "#del" + id));

        // 逻辑删除：实体上有 @TableLogic，实际执行 UPDATE user SET deleted=1
        userMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, UserResetPasswordRequest request) {
        User user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    /** 校验角色代码存在于角色字典 */
    private void validateRoleCode(String roleCode) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode)
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不存在：" + roleCode);
        }
    }

    /** 按 id 查用户，并做「存在 + 可见范围（本商户或全局顾客）」校验 */
    private User findUser(Long id) {
        User user = userMapper.selectById(id);

        // 本商户用户（merchant_id 匹配）或全局顾客（merchant_id 为空）才可操作
        long merchantId = loginContext.merchantId();
        if (user == null
                || (user.getMerchantId() != null
                    && !Objects.equals(user.getMerchantId(), merchantId))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /** 实体 → 返回 DTO（顺手过滤掉 passwordHash） */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .merchantId(user.getMerchantId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
