package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排班 Mapper（数据访问层）
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}
