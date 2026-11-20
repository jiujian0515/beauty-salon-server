package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 顾客实体（与数据库 customer 表一一对应）
 *
 * <p>为什么顾客要单独一张表，而不是和员工共用 user 表？</p>
 * <ul>
 *   <li>来源不同：员工由管理员在后台创建；顾客既能后台创建，也能在顾客端自助注册。</li>
 *   <li>字段会分化：顾客后续要加生日、肤质、会员等级、累计消费等字段，
 *       这些字段对员工毫无意义，混在一张表里会越来越脏。</li>
 *   <li>归属不同：员工必须绑定商户；顾客全局共享（不绑定商户，一个账号所有店通用）。</li>
 * </ul>
 *
 * <p>注意：顾客不参与后台角色体系（没有 user_role 关联），
 * 顾客登录后的身份固定是「顾客」，鉴权用 {@code @SaCheckLogin} 即可。</p>
 */
@Data
@TableName("customer")
public class Customer {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（登录账号，顾客表内唯一；允许与 user 表同号） */
    private String phone;

    /** 密码哈希（BCrypt，绝不存明文） */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 逻辑删除标记：1已删除 0正常（查询自动追加 deleted=0） */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
