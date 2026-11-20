package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（与数据库 user 表一一对应）
 *
 * <p>注解说明：</p>
 * <ul>
 *   <li>{@code @Data}：Lombok 自动生成 getter/setter/toString 等样板代码。</li>
 *   <li>{@code @TableName("user")}：指定对应的数据库表名。</li>
 *   <li>{@code @TableId(type = IdType.AUTO)}：主键自增。</li>
 * </ul>
 *
 * <p>字段命名约定：数据库用下划线（password_hash），Java 用驼峰（passwordHash），
 * MyBatis-Plus 会自动做映射（application.yml 里配了 map-underscore-to-camel-case）。</p>
 *
 * <p>注意：用户与角色是「多对多」关系，通过 {@code user_role} 关联表维护
 * （参考若依 RuoYi 的 sys_user_role），本实体不再包含 role 字段。</p>
 */
@Data
@TableName("user")
public class User {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户（顾客可为空） */
    private Long merchantId;

    /** 手机号（登录账号） */
    private String phone;

    /** 密码哈希（BCrypt，绝不存明文） */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /**
     * 逻辑删除标记：1已删除 0正常
     *
     * <p>{@code @TableLogic}：声明这是逻辑删除字段。加上它之后：</p>
     * <ul>
     *   <li>{@code deleteById(id)} 会变成 {@code UPDATE user SET deleted=1 ...}（不是真删）</li>
     *   <li>所有查询自动追加 {@code AND deleted=0}，被删的数据查不出来</li>
     * </ul>
     */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
