package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.ServiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务项目 Mapper（数据访问层）
 */
@Mapper
public interface ServiceItemMapper extends BaseMapper<ServiceItem> {
}
