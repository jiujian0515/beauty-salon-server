package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.CustomerCreateRequest;
import com.beautysalon.model.dto.CustomerResetPasswordRequest;
import com.beautysalon.model.dto.CustomerResponse;
import com.beautysalon.model.dto.CustomerUpdateRequest;
import com.beautysalon.model.entity.Customer;

/**
 * 顾客管理服务（后台使用：管理员对顾客的增删改查）
 *
 * <p>顾客数据全局共享，不按商户隔离——一个顾客可以在任何门店消费，
 * 各门店看到的都是同一份顾客资料。</p>
 *
 * <p>顾客来源有两个：</p>
 * <ol>
 *   <li>顾客端自助注册（{@link CustomerAuthService#register}）</li>
 *   <li>后台管理员手动添加（{@link #create}）</li>
 * </ol>
 */
public interface CustomerService {

    /**
     * 分页查询顾客
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 关键字（模糊匹配手机号或姓名，可为空）
     * @return 分页结果
     */
    PageResult<CustomerResponse> page(int page, int size, String keyword);

    /** 查询顾客详情 */
    CustomerResponse detail(Long id);

    /** 后台新增顾客 */
    CustomerResponse create(CustomerCreateRequest request);

    /** 编辑顾客（局部更新） */
    CustomerResponse update(Long id, CustomerUpdateRequest request);

    /** 删除顾客（逻辑删除） */
    void delete(Long id);

    /** 重置顾客密码 */
    void resetPassword(Long id, CustomerResetPasswordRequest request);

    /**
     * 创建顾客账号（顾客端注册与后台新增共用）
     *
     * @param phone       手机号（顾客表内唯一）
     * @param rawPassword 明文密码（内部会 BCrypt 加密）
     * @param nickname    昵称
     * @param status      状态（null 视为 1 启用）
     * @return 新建的顾客实体（含自增 id）
     */
    Customer createAccount(String phone, String rawPassword, String nickname, Integer status);
}
