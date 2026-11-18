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
 * 其中 {@code code} 与 {@code user.role} 字段对应，是权限判断的关键。</p>
 */
@Data
@TableName("role")
public class Role {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称（中文显示用，如「管理员」「员工」「顾客」） */
    private String name;

    /** 角色代码（如 CUSTOMER/STAFF/ADMIN，与 user.role 字段对应） */
    private String code;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
