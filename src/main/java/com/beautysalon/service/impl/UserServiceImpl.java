package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.RoleResponse;
import com.beautysalon.model.dto.UserCreateRequest;
import com.beautysalon.model.dto.UserResetPasswordRequest;
import com.beautysalon.model.dto.UserResponse;
import com.beautysalon.model.dto.UserUpdateRequest;
import com.beautysalon.model.entity.Role;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.UserRoleService;
import com.beautysalon.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户账号管理实现
 *
 * <p>三条核心规则（与若依 RuoYi 一致）：</p>
 * <ol>
 *   <li>用户与角色是「多对多」，角色关系全部通过 {@link UserRoleService} 维护，
 *       {@code user} 表本身不再有 role 列。</li>
 *   <li>数据隔离：本模块只管理「管理员/员工」，按商户隔离（只能看到本商户的用户）。
 *       顾客是独立账号体系（customer 表），由顾客管理模块维护，见
 *       {@link com.beautysalon.service.CustomerService}。</li>
 *   <li>管理员保护：ADMIN 账号的角色不允许被修改，也不允许被删除/禁用，
 *       避免把系统里唯一的管理员「改废」。</li>
 * </ol>
 */
@Service
public class UserServiceImpl implements UserService {

    /** 管理员角色代码（内置角色，受保护） */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 顾客角色代码（顾客已独立成 customer 表，后台账号不允许再挂这个角色） */
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final LoginContext loginContext;

    public UserServiceImpl(UserMapper userMapper,
                           UserRoleService userRoleService,
                           PasswordEncoder passwordEncoder,
                           LoginContext loginContext) {
        this.userMapper = userMapper;
        this.userRoleService = userRoleService;
        this.passwordEncoder = passwordEncoder;
        this.loginContext = loginContext;
    }

    /**
     * 分页查询（只查本商户的管理员/员工）
     *
     * <p>筛选条件：keyword 同时模糊匹配手机号与姓名；role 按角色代码筛选，
     * 通过 EXISTS 子查询命中 user_role 关联表。</p>
     */
    @Override
    public PageResult<UserResponse> page(int page, int size, String keyword, String role) {
        long merchantId = loginContext.merchantId();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMerchantId, merchantId);

        if (StringUtils.hasText(role)) {
            // {0} 是 MyBatis-Plus 的占位符，会被绑定为预编译参数，不存在 SQL 注入风险
            wrapper.exists("SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id "
                    + "WHERE ur.user_id = user.id AND r.code = {0}", role.trim());
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, kw).or().like(User::getNickname, kw));
        }

        wrapper.orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查角色，避免每行都查一次库（N+1）
        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, List<Role>> roleMap = userRoleService.rolesByUserIds(userIds);

        List<UserResponse> records = result.getRecords().stream()
                .map(user -> toResponse(user, roleMap.getOrDefault(user.getId(), List.of())))
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public UserResponse detail(Long id) {
        User user = findUser(id);
        return toResponse(user, userRoleService.rolesOf(id));
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        long merchantId = loginContext.merchantId();

        // 先校验角色合法性，再落库，避免产生「有用户没角色」的脏数据
        List<Role> roles = userRoleService.resolveRoles(request.getRoleIds());
        checkStaffRoles(roles);

        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已存在");
        }

        User user = new User();
        // 后台账号（管理员/员工）必须绑定当前商户
        user.setMerchantId(merchantId);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        userMapper.insert(user);

        userRoleService.replaceRoles(user.getId(), request.getRoleIds());

        // 重新查一次，把数据库默认值（created_at）带给前端
        return toResponse(userMapper.selectById(user.getId()), userRoleService.rolesOf(user.getId()));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);

        boolean targetAdmin = userRoleService.hasRole(id, ROLE_ADMIN);
        boolean self = Objects.equals(id, loginContext.userId());

        // 管理员账号只允许本人维护，其他账号不能改，防止越权接管管理员
        if (targetAdmin && !self) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "不允许修改其他管理员账号");
        }

        // 角色变更先校验，避免手机号等信息已改、角色却校验失败（非事务场景）
        if (request.getRoleIds() != null) {
            if (targetAdmin) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员账号的角色不允许修改");
            }
            checkStaffRoles(userRoleService.resolveRoles(request.getRoleIds()));
        }

        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getPhone())
                    .ne(User::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该手机号已存在");
            }
            user.setPhone(request.getPhone());
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }

        if (request.getStatus() != null) {
            if (targetAdmin && request.getStatus() != 1) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员账号不允许禁用");
            }
            user.setStatus(request.getStatus());
        }

        userMapper.updateById(user);

        if (request.getRoleIds() != null) {
            userRoleService.replaceRoles(id, request.getRoleIds());
        }

        return toResponse(user, userRoleService.rolesOf(id));
    }

    @Override
    public void delete(Long id) {
        User user = findUser(id);

        if (userRoleService.hasRole(id, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员账号不允许删除");
        }
        if (Objects.equals(id, loginContext.userId())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "不能删除当前登录账号");
        }

        // 逻辑删除后手机号仍占用唯一索引，先改名释放，保证同号可重新注册
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getPhone, user.getPhone() + "#del" + id));

        userRoleService.removeByUserId(id);
        userMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, UserResetPasswordRequest request) {
        User user = findUser(id);

        boolean targetAdmin = userRoleService.hasRole(id, ROLE_ADMIN);
        boolean self = Objects.equals(id, loginContext.userId());
        if (targetAdmin && !self) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "不允许修改其他管理员账号的密码");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 查询用户并校验可见范围
     *
     * <p>只允许操作本商户的账号，其他商户的账号一律当作不存在（避免泄露数据）。</p>
     */
    private User findUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        long merchantId = loginContext.merchantId();
        if (!Objects.equals(user.getMerchantId(), merchantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 校验角色只能是「后台角色」（管理员/员工）
     *
     * <p>顾客已经是独立账号体系（customer 表 + 顾客端注册），
     * 后台账号再挂 CUSTOMER 角色会造成两套体系混乱，所以直接拒绝。</p>
     */
    private void checkStaffRoles(List<Role> roles) {
        boolean containsCustomer = roles.stream()
                .anyMatch(role -> ROLE_CUSTOMER.equals(role.getCode()));
        if (containsCustomer) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "CUSTOMER 是顾客专属角色，顾客请在「顾客管理」中维护");
        }
    }

    /** 实体 → 返回 DTO（角色由调用方批量查好后传入） */
    private UserResponse toResponse(User user, List<Role> roles) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .merchantId(user.getMerchantId())
                .roles(roles.stream().map(RoleResponse::of).toList())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
