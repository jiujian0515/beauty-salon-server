package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联 Mapper（数据访问层）
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
