package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.CalendarDayResponse;
import com.beautysalon.model.dto.ScheduleBatchCreateRequest;
import com.beautysalon.model.dto.ScheduleBatchDeleteResponse;
import com.beautysalon.model.dto.ScheduleBatchResponse;
import com.beautysalon.model.dto.ScheduleCreateRequest;
import com.beautysalon.model.dto.ScheduleResponse;
import com.beautysalon.model.dto.ScheduleUpdateRequest;
import com.beautysalon.model.dto.SlotInventoryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务
 */
public interface ScheduleService {

    /** 分页查询排班 */
    PageResult<ScheduleResponse> page(int page, int size, Long resourceId,
                                      LocalDate startDate, LocalDate endDate, Integer status);

    /** 排班详情 */
    ScheduleResponse detail(Long id);

    /** 新增排班，并自动生成时段库存 */
    ScheduleResponse create(ScheduleCreateRequest request);

    /** 批量新增排班 */
    ScheduleBatchResponse batchCreate(ScheduleBatchCreateRequest request);

    /** 编辑排班 */
    ScheduleResponse update(Long id, ScheduleUpdateRequest request);

    /** 删除排班（有预约占用时禁止） */
    void delete(Long id);

    /** 批量删除排班，有预约占用的会被跳过 */
    ScheduleBatchDeleteResponse batchDelete(List<Long> ids);

    /** 查询某条排班窗口内的时段库存 */
    List<SlotInventoryResponse> slots(Long id);

    /** 日历汇总 */
    List<CalendarDayResponse> calendar(LocalDate startDate, LocalDate endDate, Long resourceId);
}
