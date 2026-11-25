package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.dto.ReservationStatsResponse;
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
     * 统计指定顾客的预约数量：累计成功（已完成）、待服务、待支付
     *
     * <p>单条聚合查询替代三次 COUNT，无记录时三项均为 0。</p>
     */
    @Select("""
            SELECT
              COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END)       AS completedCount,
              COUNT(CASE WHEN status = 'CONFIRMED' THEN 1 END)       AS pendingServiceCount,
              COUNT(CASE WHEN status = 'PENDING_PAYMENT' THEN 1 END) AS pendingPaymentCount
            FROM reservation
            WHERE customer_id = #{customerId}
            """)
    ReservationStatsResponse countStatsByCustomerId(@Param("customerId") Long customerId);

    /** 统计商户内某核销码是否已存在（生成核销码时去重） */
    @Select("""
            SELECT COUNT(*)
            FROM reservation
            WHERE merchant_id = #{merchantId} AND verify_code = #{verifyCode}
            """)
    long countByMerchantAndVerifyCode(@Param("merchantId") Long merchantId,
                                      @Param("verifyCode") String verifyCode);

    /** 按商户 + 核销码查询预约单（商户数据隔离） */
    @Select("""
            SELECT *
            FROM reservation
            WHERE merchant_id = #{merchantId} AND verify_code = #{verifyCode}
            LIMIT 1
            """)
    Reservation selectByMerchantAndVerifyCode(@Param("merchantId") Long merchantId,
                                              @Param("verifyCode") String verifyCode);

    /**
     * 核销预约：CONFIRMED → COMPLETED
     *
     * <p>原子更新，重复核销或状态不符时影响行数为 0。</p>
     */
    @Update("""
            UPDATE reservation
            SET status = 'COMPLETED', verified_at = NOW(), verified_by = #{operatorId}
            WHERE merchant_id = #{merchantId}
              AND verify_code = #{verifyCode}
              AND status = 'CONFIRMED'
            """)
    int verifyByCode(@Param("merchantId") Long merchantId,
                     @Param("verifyCode") String verifyCode,
                     @Param("operatorId") Long operatorId);

    /**
     * 支付成功确认预约：只有 PENDING_PAYMENT 才能流转，重复回调影响行数为 0。
     */
    @Update("""
            UPDATE reservation
            SET status = 'CONFIRMED', paid_at = NOW()
            WHERE id = #{id} AND status = 'PENDING_PAYMENT'
            """)
    int confirmPaid(@Param("id") Long id);

    /** 发起退款：CONFIRMED → REFUNDING（原子，重复操作影响行数为 0） */
    @Update("UPDATE reservation SET status = 'REFUNDING' WHERE id = #{id} AND status = 'CONFIRMED'")
    int markRefunding(@Param("id") Long id);

    /** 退款完成：REFUNDING → REFUNDED，同时记录取消时间 */
    @Update("""
            UPDATE reservation
            SET status = 'REFUNDED', cancelled_at = NOW()
            WHERE id = #{id} AND status = 'REFUNDING'
            """)
    int markRefunded(@Param("id") Long id);

    /** 直接取消：fromStatus → CANCELLED（用于未支付或无需退款） */
    @Update("""
            UPDATE reservation
            SET status = 'CANCELLED', cancelled_at = NOW()
            WHERE id = #{id} AND status = #{fromStatus}
            """)
    int markCancelled(@Param("id") Long id, @Param("fromStatus") String fromStatus);
}
