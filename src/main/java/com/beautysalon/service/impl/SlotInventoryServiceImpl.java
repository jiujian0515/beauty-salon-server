package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.ResourceMapper;
import com.beautysalon.mapper.ScheduleMapper;
import com.beautysalon.mapper.SlotInventoryMapper;
import com.beautysalon.model.dto.AvailableSlotResponse;
import com.beautysalon.model.dto.SlotInventoryResponse;
import com.beautysalon.model.entity.Resource;
import com.beautysalon.model.entity.Schedule;
import com.beautysalon.model.entity.SlotInventory;
import com.beautysalon.service.SlotInventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 时段库存实现
 */
@Service
public class SlotInventoryServiceImpl implements SlotInventoryService {

    private static final int SLOT_SELLABLE = 1;
    private static final int SLOT_LOCKED = 0;
    private static final int SCHEDULE_ACTIVE = 1;
    private static final int RESOURCE_ENABLED = 1;

    private final SlotInventoryMapper slotInventoryMapper;
    private final ScheduleMapper scheduleMapper;
    private final ResourceMapper resourceMapper;
    private final LoginContext loginContext;
    private final ZoneId zoneId;
    private final int leadHours;

    public SlotInventoryServiceImpl(SlotInventoryMapper slotInventoryMapper,
                                    ScheduleMapper scheduleMapper,
                                    ResourceMapper resourceMapper,
                                    LoginContext loginContext,
                                    @Value("${app.timezone:Asia/Shanghai}") String timezone,
                                    @Value("${app.booking.lead-hours:24}") int leadHours) {
        this.slotInventoryMapper = slotInventoryMapper;
        this.scheduleMapper = scheduleMapper;
        this.resourceMapper = resourceMapper;
        this.loginContext = loginContext;
        this.zoneId = ZoneId.of(timezone);
        this.leadHours = leadHours;
    }

    @Override
    public PageResult<SlotInventoryResponse> page(int page, int size, Long resourceId,
                                                  LocalDate startDate, LocalDate endDate, Integer status) {
        long merchantId = loginContext.merchantId();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<SlotInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlotInventory::getMerchantId, merchantId);
        if (resourceId != null) {
            wrapper.eq(SlotInventory::getResourceId, resourceId);
        }
        if (startDate != null) {
            wrapper.ge(SlotInventory::getSlotDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(SlotInventory::getSlotDate, endDate);
        }
        if (status != null) {
            wrapper.eq(SlotInventory::getStatus, status);
        }
        wrapper.orderByAsc(SlotInventory::getSlotDate).orderByAsc(SlotInventory::getStartTime);

        Page<SlotInventory> result = slotInventoryMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Resource> resourceMap = loadResources(result.getRecords());

        List<SlotInventoryResponse> records = result.getRecords().stream()
                .map(slot -> toResponse(slot, resourceMap.get(slot.getResourceId())))
                .toList();
        return PageResult.of(result, records);
    }

    @Override
    public List<AvailableSlotResponse> availableSlots(LocalDate date, Long resourceId, Long merchantId) {
        if (date == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期不能为空");
        }
        if (date.isBefore(LocalDate.now(zoneId))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期不能早于今天");
        }

        Long effectiveMerchantId = merchantId;
        if (resourceId != null) {
            Resource resource = resourceMapper.selectById(resourceId);
            if (resource == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
            }
            if (effectiveMerchantId != null
                    && !Objects.equals(resource.getMerchantId(), effectiveMerchantId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "资源不属于该商户");
            }
            effectiveMerchantId = resource.getMerchantId();
        }

        LambdaQueryWrapper<SlotInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlotInventory::getSlotDate, date)
                .eq(SlotInventory::getStatus, SLOT_SELLABLE)
                .apply("used < total");
        if (resourceId != null) {
            wrapper.eq(SlotInventory::getResourceId, resourceId);
        }
        if (effectiveMerchantId != null) {
            wrapper.eq(SlotInventory::getMerchantId, effectiveMerchantId);
        }
        wrapper.orderByAsc(SlotInventory::getResourceId)
                .orderByAsc(SlotInventory::getStartTime);

        List<SlotInventory> slots = slotInventoryMapper.selectList(wrapper);
        if (slots.isEmpty()) {
            return List.of();
        }

        LocalDateTime earliestStart = LocalDateTime.now(zoneId).plusHours(leadHours);
        Map<Long, Resource> resourceMap = loadResources(slots);
        return slots.stream()
                .filter(slot -> slot.getStartTime() != null
                        && !LocalDateTime.of(slot.getSlotDate(), slot.getStartTime())
                        .isBefore(earliestStart))
                .map(slot -> {
                    Resource resource = resourceMap.get(slot.getResourceId());
                    if (resource == null || resource.getStatus() == null
                            || resource.getStatus() != RESOURCE_ENABLED) {
                        return null;
                    }
                    return toAvailableResponse(slot, resource);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public SlotInventoryResponse lock(Long id) {
        SlotInventory slot = findSlot(id);
        if (slot.getStatus() != null && slot.getStatus() == SLOT_LOCKED) {
            return toResponse(slot, findResource(slot.getResourceId(), slot.getMerchantId()));
        }
        slotInventoryMapper.updateStatusById(slot.getId(), SLOT_LOCKED);
        SlotInventory saved = slotInventoryMapper.selectById(id);
        return toResponse(saved, findResource(saved.getResourceId(), saved.getMerchantId()));
    }

    @Override
    @Transactional
    public SlotInventoryResponse unlock(Long id) {
        SlotInventory slot = findSlot(id);
        Schedule schedule = findScheduleForSlot(slot);
        if (schedule == null || schedule.getStatus() == null
                || schedule.getStatus() != SCHEDULE_ACTIVE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该时段所属排班无效，不能释放，请先启用排班");
        }
        if (slot.getStatus() != null && slot.getStatus() == SLOT_SELLABLE) {
            return toResponse(slot, findResource(slot.getResourceId(), slot.getMerchantId()));
        }
        slotInventoryMapper.updateStatusById(slot.getId(), SLOT_SELLABLE);
        SlotInventory saved = slotInventoryMapper.selectById(id);
        return toResponse(saved, findResource(saved.getResourceId(), saved.getMerchantId()));
    }

    private SlotInventory findSlot(Long id) {
        SlotInventory slot = slotInventoryMapper.selectById(id);
        if (slot == null
                || !Objects.equals(slot.getMerchantId(), loginContext.merchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "时段不存在");
        }
        return slot;
    }

    /** 库存表没有 schedule_id，按资源 + 日期 + 时间落入窗口定位所属排班 */
    private Schedule findScheduleForSlot(SlotInventory slot) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getResourceId, slot.getResourceId())
                .eq(Schedule::getScheduleDate, slot.getSlotDate())
                .le(Schedule::getStartTime, slot.getStartTime())
                .gt(Schedule::getEndTime, slot.getStartTime())
                .orderByDesc(Schedule::getId)
                .last("LIMIT 1");
        return scheduleMapper.selectOne(wrapper);
    }

    private Resource findResource(Long resourceId, long merchantId) {
        if (resourceId == null) {
            return null;
        }
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !Objects.equals(resource.getMerchantId(), merchantId)) {
            return null;
        }
        return resource;
    }

    private Map<Long, Resource> loadResources(List<SlotInventory> slots) {
        List<Long> resourceIds = slots.stream()
                .map(SlotInventory::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        return resourceMapper.selectBatchIds(resourceIds).stream()
                .collect(Collectors.toMap(Resource::getId, resource -> resource));
    }

    private AvailableSlotResponse toAvailableResponse(SlotInventory slot, Resource resource) {
        int total = slot.getTotal() == null ? 0 : slot.getTotal();
        int used = slot.getUsed() == null ? 0 : slot.getUsed();
        return AvailableSlotResponse.builder()
                .id(slot.getId())
                .merchantId(slot.getMerchantId())
                .resourceId(slot.getResourceId())
                .resourceName(resource.getName())
                .resourceType(resource.getType())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .total(total)
                .used(used)
                .available(Math.max(total - used, 0))
                .build();
    }

    private SlotInventoryResponse toResponse(SlotInventory slot, Resource resource) {
        int total = slot.getTotal() == null ? 0 : slot.getTotal();
        int used = slot.getUsed() == null ? 0 : slot.getUsed();
        return SlotInventoryResponse.builder()
                .id(slot.getId())
                .merchantId(slot.getMerchantId())
                .resourceId(slot.getResourceId())
                .resourceName(resource == null ? null : resource.getName())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .total(total)
                .used(used)
                .available(Math.max(total - used, 0))
                .version(slot.getVersion())
                .status(slot.getStatus())
                .createdAt(slot.getCreatedAt())
                .build();
    }
}
