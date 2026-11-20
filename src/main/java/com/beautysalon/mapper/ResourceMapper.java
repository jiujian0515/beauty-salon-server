package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资源 Mapper（数据访问层）
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    /**
     * 统计资源被业务数据引用的次数（排班 / 时段库存 / 预约）
     *
     * <p>删除资源前调用：只要还有任何引用，就不允许删除，
     * 避免排班、库存、预约出现「孤儿」数据。</p>
     */
    @Select("SELECT "
            + "(SELECT COUNT(*) FROM schedule WHERE resource_id = #{resourceId}) "
            + "+ (SELECT COUNT(*) FROM slot_inventory WHERE resource_id = #{resourceId}) "
            + "+ (SELECT COUNT(*) FROM reservation WHERE resource_id = #{resourceId})")
    long countReferences(@Param("resourceId") Long resourceId);
}
