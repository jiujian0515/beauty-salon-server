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
}
