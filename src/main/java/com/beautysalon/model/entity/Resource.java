package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源实体（与数据库 resource 表一一对应）
 *
 * <p>资源分两类：</p>
 * <ul>
 *   <li>{@code STAFF}：技师，必须绑定一个非管理员用户（{@link #userId}）</li>
 *   <li>{@code ROOM}：床位/房间，不绑定用户</li>
 * </ul>
 */
@Data
@TableName("resource")
public class Resource {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 类型：STAFF 技师 / ROOM 床位 */
    private String type;

    /** 绑定用户 id（仅 STAFF 有值） */
    private Long userId;

    /** 资源名称 */
    private String name;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
