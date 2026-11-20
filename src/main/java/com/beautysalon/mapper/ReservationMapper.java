package com.beautysalon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 预约 Mapper（当前阶段只提供库存引用统计，预约模块后续补齐完整实体）
 */
@Mapper
public interface ReservationMapper {

    /**
     * 统计这些时段是否产生过预约（包含已取消等历史记录）
     *
     * <p>排班改时间或删除前调用：只要产生过预约，就不允许删掉对应库存，
     * 否则历史预约单会指向不存在的时段。</p>
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM reservation
            WHERE slot_inventory_id IN
            <foreach collection="slotIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    long countBySlotIds(@Param("slotIds") List<Long> slotIds);
}
