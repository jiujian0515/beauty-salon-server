package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（数据访问层）
 *
 * <p>继承 {@link BaseMapper} 后，自动拥有通用增删改查方法：</p>
 * <ul>
 *   <li>selectById / selectOne / selectList（查询）</li>
 *   <li>insert（新增）</li>
 *   <li>updateById / update（更新）</li>
 *   <li>deleteById（删除）</li>
 * </ul>
 *
 * <p>这些方法由 MyBatis-Plus 在运行时动态生成，无需写 SQL。
 * 只有复杂查询才需要在这里声明方法 + 写 Mapper XML。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 简单查询直接用 BaseMapper 提供的方法即可，这里暂时为空
}
