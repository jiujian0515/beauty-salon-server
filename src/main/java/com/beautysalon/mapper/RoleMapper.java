package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper（数据访问层）
 *
 * <p>继承 {@link BaseMapper} 后自动拥有通用增删改查方法，
 * 简单查询无需写 SQL。</p>
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
