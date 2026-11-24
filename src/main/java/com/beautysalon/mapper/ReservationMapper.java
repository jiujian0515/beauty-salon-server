package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 预约 Mapper（数据访问层）
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

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

    /**
     * 支付成功确认预约：只有 PENDING_PAYMENT 才能流转，重复回调影响行数为 0。
     */
    @Update("""
            UPDATE reservation
            SET status = 'CONFIRMED', paid_at = NOW()
            WHERE id = #{id} AND status = 'PENDING_PAYMENT'
            """)
    int confirmPaid(@Param("id") Long id);
}
