package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 顾客 Mapper（数据访问层）
 *
 * <p>继承 {@link BaseMapper} 后自动拥有 selectById / insert / updateById / deleteById 等通用方法，
 * 简单查询无需写 SQL。</p>
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
