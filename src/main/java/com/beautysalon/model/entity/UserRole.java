package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联实体（与数据库 user_role 表一一对应）
 *
 * <p>参考若依 RuoYi 的 sys_user_role：用户和角色是多对多关系，
 * 一个用户可以拥有多个角色，一个角色也可以分配给多个用户。</p>
 */
@Data
@TableName("user_role")
public class UserRole {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id */
    private Long userId;

    /** 角色 id */
    private Long roleId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
