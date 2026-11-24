package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.DepositTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 定金流水 Mapper（数据访问层）
 */
@Mapper
public interface DepositTransactionMapper extends BaseMapper<DepositTransaction> {

    /** 按平台交易号查询（支付回调幂等键） */
    @Select("SELECT * FROM deposit_transaction WHERE out_trade_no = #{outTradeNo} LIMIT 1")
    DepositTransaction selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /**
     * 原子置为支付成功：只有 PENDING 才能流转，重复回调影响行数为 0。
     */
    @Update("""
            UPDATE deposit_transaction
            SET status = 'SUCCESS', paid_at = NOW()
            WHERE out_trade_no = #{outTradeNo} AND status = 'PENDING'
            """)
    int markSuccess(@Param("outTradeNo") String outTradeNo);

    /** 按预约单查询定金流水（一次预约最多一条） */
    @Select("SELECT * FROM deposit_transaction WHERE reservation_id = #{reservationId} LIMIT 1")
    DepositTransaction selectByReservationId(@Param("reservationId") Long reservationId);

    /** 退款成功：SUCCESS → REFUNDED，同时记录退款时间 */
    @Update("""
            UPDATE deposit_transaction
            SET status = 'REFUNDED', refunded_at = NOW()
            WHERE reservation_id = #{reservationId} AND status = 'SUCCESS'
            """)
    int markRefunded(@Param("reservationId") Long reservationId);

    /** 未支付订单取消：PENDING → EXPIRED */
    @Update("""
            UPDATE deposit_transaction
            SET status = 'EXPIRED'
            WHERE reservation_id = #{reservationId} AND status = 'PENDING'
            """)
    int markExpired(@Param("reservationId") Long reservationId);
}
