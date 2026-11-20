package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.SlotInventoryResponse;

import java.time.LocalDate;

/**
 * 时段库存服务
 */
public interface SlotInventoryService {

    /** 分页查询时段库存 */
    PageResult<SlotInventoryResponse> page(int page, int size, Long resourceId,
                                           LocalDate startDate, LocalDate endDate, Integer status);

    /** 锁定时段（暂停售卖） */
    SlotInventoryResponse lock(Long id);

    /** 释放时段 */
    SlotInventoryResponse unlock(Long id);
}
