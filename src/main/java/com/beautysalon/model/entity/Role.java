package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（与数据库 role 表一一对应，角色字典）
 *
 * <p>角色表是全局字典，不绑定商户（所有商户共用同一套角色定义），
 * 用户与角色通过 {@code user_role} 关联表建立多对多关系，
 * 其中 {@code code}（如 ADMIN）是权限判断的关键（{@code @SaCheckRole("ADMIN")}）。</p>
 */
@Data
@TableName("role")
public class Role {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称（中文显示用，如「管理员」「员工」「顾客」） */
    private String name;

    /** 角色代码（如 CUSTOMER/STAFF/ADMIN，权限判断用） */
    private String code;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
