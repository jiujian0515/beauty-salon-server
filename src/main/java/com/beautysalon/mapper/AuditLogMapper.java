package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper（数据访问层）
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
