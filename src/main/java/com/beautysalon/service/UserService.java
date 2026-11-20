package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.UserCreateRequest;
import com.beautysalon.model.dto.UserResetPasswordRequest;
import com.beautysalon.model.dto.UserResponse;
import com.beautysalon.model.dto.UserUpdateRequest;

/**
 * 用户账号管理服务（管理员/员工，增删改查 + 重置密码）
 *
 * <p>本模块只管理「后台账号」（user 表）：管理员 / 员工，必须绑定商户、按商户隔离。</p>
 * <p>顾客是独立账号体系（customer 表，全局共享，支持顾客端自助注册），
 * 请使用 {@link CustomerService}。</p>
 */
public interface UserService {

    /**
     * 分页查询用户列表
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 关键字（模糊匹配手机号或姓名，可为空）
     * @param role    角色代码筛选（ADMIN/STAFF/CUSTOMER，可为空）
     * @return 分页结果
     */
    PageResult<UserResponse> page(int page, int size, String keyword, String role);

    /** 查询用户详情 */
    UserResponse detail(Long id);

    /** 新增用户（创建后即可用手机号 + 密码登录） */
    UserResponse create(UserCreateRequest request);

    /** 编辑用户（局部更新：只改传了的字段） */
    UserResponse update(Long id, UserUpdateRequest request);

    /** 删除用户（逻辑删除，数据保留可追溯） */
    void delete(Long id);

    /** 重置用户密码 */
    void resetPassword(Long id, UserResetPasswordRequest request);
}
