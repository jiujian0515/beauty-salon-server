package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.AvailableSlotResponse;
import com.beautysalon.model.dto.SlotInventoryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 时段库存服务
 */
public interface SlotInventoryService {

    /** 分页查询时段库存 */
    PageResult<SlotInventoryResponse> page(int page, int size, Long resourceId,
                                           LocalDate startDate, LocalDate endDate, Integer status);

    /**
     * 顾客端可售时段查询
     *
     * @param date       查询日期（必填）
     * @param resourceId 资源 id（可选）
     * @param merchantId 商户 id（可选，不传 resourceId 时用于限定商户）
     * @return 只包含资源启用、时段可售、还有余量且满足预约提前量的时段
     */
    List<AvailableSlotResponse> availableSlots(LocalDate date, Long resourceId, Long merchantId);

    /** 锁定时段（暂停售卖） */
    SlotInventoryResponse lock(Long id);

    /** 释放时段 */
    SlotInventoryResponse unlock(Long id);
}
