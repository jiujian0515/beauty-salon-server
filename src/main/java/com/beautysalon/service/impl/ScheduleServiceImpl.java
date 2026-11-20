package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.ReservationMapper;
import com.beautysalon.mapper.ResourceMapper;
import com.beautysalon.mapper.ScheduleMapper;
import com.beautysalon.mapper.SlotInventoryMapper;
import com.beautysalon.model.dto.CalendarDayResponse;
import com.beautysalon.model.dto.ScheduleBatchCreateRequest;
import com.beautysalon.model.dto.ScheduleBatchDeleteResponse;
import com.beautysalon.model.dto.ScheduleBatchResponse;
import com.beautysalon.model.dto.ScheduleCreateRequest;
import com.beautysalon.model.dto.ScheduleResponse;
import com.beautysalon.model.dto.ScheduleUpdateRequest;
import com.beautysalon.model.dto.SlotInventoryResponse;
import com.beautysalon.model.entity.Resource;
import com.beautysalon.model.entity.Schedule;
import com.beautysalon.model.entity.SlotInventory;
import com.beautysalon.service.ScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排班管理实现
 *
 * <p>核心规则：</p>
 * <ol>
 *   <li>所有操作只针对当前登录用户所属商户。</li>
 *   <li>保存排班时按固定步长自动生成时段库存，重复执行不会重置已占用数。</li>
 *   <li>同一资源同一天的排班时间窗口不能重叠。</li>
 *   <li>已有预约占用或预约历史时，禁止改日期/时间/拆分步长，禁止删除。</li>
 *   <li>容量不能小于窗口内已占用数。</li>
 * </ol>
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final int DEFAULT_CAPACITY = 1;
    private static final int DEFAULT_SLOT_MINUTES = 60;
    private static final int MIN_SLOT_MINUTES = 5;
    private static final int MAX_SLOT_MINUTES = 480;
    private static final int MAX_BATCH_DAYS = 90;
    private static final int MAX_CALENDAR_DAYS = 62;

    private static final int STATUS_ACTIVE = 1;
    private static final int SLOT_SELLABLE = 1;
    private static final int SLOT_LOCKED = 0;

    private final ScheduleMapper scheduleMapper;
    private final SlotInventoryMapper slotInventoryMapper;
    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final LoginContext loginContext;
    private final ZoneId zoneId;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper,
                               SlotInventoryMapper slotInventoryMapper,
                               ReservationMapper reservationMapper,
                               ResourceMapper resourceMapper,
                               LoginContext loginContext,
                               @Value("${app.timezone:Asia/Shanghai}") String timezone) {
        this.scheduleMapper = scheduleMapper;
        this.slotInventoryMapper = slotInventoryMapper;
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.loginContext = loginContext;
        this.zoneId = ZoneId.of(timezone);
    }

    @Override
    public PageResult<ScheduleResponse> page(int page, int size, Long resourceId,
                                             LocalDate startDate, LocalDate endDate, Integer status) {
        long merchantId = loginContext.merchantId();
        validateDateRange(startDate, endDate);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getMerchantId, merchantId);
        if (resourceId != null) {
            wrapper.eq(Schedule::getResourceId, resourceId);
        }
        if (startDate != null) {
            wrapper.ge(Schedule::getScheduleDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Schedule::getScheduleDate, endDate);
        }
        if (status != null) {
            wrapper.eq(Schedule::getStatus, status);
        }
        wrapper.orderByAsc(Schedule::getScheduleDate).orderByAsc(Schedule::getStartTime);

        Page<Schedule> result = scheduleMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Resource> resourceMap = loadResources(result.getRecords());
        Map<ResourceDateKey, List<SlotInventory>> slotMap = loadSlotsForSchedules(result.getRecords());

        List<ScheduleResponse> records = result.getRecords().stream()
                .map(schedule -> toResponse(schedule,
                        resourceMap.get(schedule.getResourceId()),
                        windowSlots(schedule, slotMap)))
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public ScheduleResponse detail(Long id) {
        Schedule schedule = findSchedule(id);
        return toResponse(schedule,
                findResource(schedule.getResourceId(), schedule.getMerchantId()),
                loadWindowSlots(schedule));
    }

    @Override
    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        long merchantId = loginContext.merchantId();
        Resource resource = validateResourceForSchedule(request.getResourceId(), merchantId);

        int capacity = request.getCapacity() == null ? DEFAULT_CAPACITY : request.getCapacity();
        int slotMinutes = request.getSlotMinutes() == null ? DEFAULT_SLOT_MINUTES : request.getSlotMinutes();
        int status = request.getStatus() == null ? STATUS_ACTIVE : request.getStatus();

        validateWindow(request.getScheduleDate(), request.getStartTime(), request.getEndTime(),
                slotMinutes, capacity);
        ensureNotPast(request.getScheduleDate());
        ensureNoOverlap(resource.getId(), request.getScheduleDate(),
                request.getStartTime(), request.getEndTime(), null);

        Schedule schedule = new Schedule();
        schedule.setMerchantId(merchantId);
        schedule.setResourceId(resource.getId());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setCapacity(capacity);
        schedule.setSlotMinutes(slotMinutes);
        schedule.setStatus(status);
        scheduleMapper.insert(schedule);

        generateSlots(schedule);

        Schedule saved = scheduleMapper.selectById(schedule.getId());
        return toResponse(saved, resource, loadWindowSlots(saved));
    }

    @Override
    @Transactional
    public ScheduleBatchResponse batchCreate(ScheduleBatchCreateRequest request) {
        long merchantId = loginContext.merchantId();
        Resource resource = validateResourceForSchedule(request.getResourceId(), merchantId);

        int capacity = request.getCapacity() == null ? DEFAULT_CAPACITY : request.getCapacity();
        int slotMinutes = request.getSlotMinutes() == null ? DEFAULT_SLOT_MINUTES : request.getSlotMinutes();
        int status = request.getStatus() == null ? STATUS_ACTIVE : request.getStatus();

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        LocalDate today = LocalDate.now(zoneId);
        if (start.isBefore(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始日期不能早于今天");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_BATCH_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "一次最多批量生成 " + MAX_BATCH_DAYS + " 天排班");
        }
        validateWindow(start, request.getStartTime(), request.getEndTime(), slotMinutes, capacity);

        Set<Integer> weekdays = normalizeWeekdays(request.getWeekdays());
        List<ScheduleBatchResponse.SkippedItem> skipped = new ArrayList<>();
        int created = 0;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!weekdays.isEmpty() && !weekdays.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            if (overlapCount(resource.getId(), date,
                    request.getStartTime(), request.getEndTime(), null) > 0) {
                skipped.add(ScheduleBatchResponse.SkippedItem.builder()
                        .date(date)
                        .reason("该日期时间窗口已有排班")
                        .build());
                continue;
            }

            Schedule schedule = new Schedule();
            schedule.setMerchantId(merchantId);
            schedule.setResourceId(resource.getId());
            schedule.setScheduleDate(date);
            schedule.setStartTime(request.getStartTime());
            schedule.setEndTime(request.getEndTime());
            schedule.setCapacity(capacity);
            schedule.setSlotMinutes(slotMinutes);
            schedule.setStatus(status);
            scheduleMapper.insert(schedule);
            generateSlots(schedule);
            created++;
        }

        return ScheduleBatchResponse.builder()
                .createdCount(created)
                .skippedCount(skipped.size())
                .skipped(skipped)
                .build();
    }

    @Override
    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        Schedule schedule = findSchedule(id);
        Resource resource = findResource(schedule.getResourceId(), schedule.getMerchantId());

        LocalDate oldDate = schedule.getScheduleDate();
        LocalTime oldStart = schedule.getStartTime();
        LocalTime oldEnd = schedule.getEndTime();
        int oldCapacity = schedule.getCapacity() == null ? DEFAULT_CAPACITY : schedule.getCapacity();
        int oldSlotMinutes = schedule.getSlotMinutes() == null ? DEFAULT_SLOT_MINUTES : schedule.getSlotMinutes();
        int oldStatus = schedule.getStatus() == null ? STATUS_ACTIVE : schedule.getStatus();

        List<SlotInventory> oldSlots = slotInventoryMapper.selectWindow(
                schedule.getResourceId(), oldDate, oldStart, oldEnd);

        LocalDate newDate = request.getScheduleDate() == null ? oldDate : request.getScheduleDate();
        LocalTime newStart = request.getStartTime() == null ? oldStart : request.getStartTime();
        LocalTime newEnd = request.getEndTime() == null ? oldEnd : request.getEndTime();
        int newCapacity = request.getCapacity() == null ? oldCapacity : request.getCapacity();
        int newSlotMinutes = request.getSlotMinutes() == null ? oldSlotMinutes : request.getSlotMinutes();
        int newStatus = request.getStatus() == null ? oldStatus : request.getStatus();

        boolean timeChanged = !Objects.equals(oldDate, newDate)
                || !Objects.equals(oldStart, newStart)
                || !Objects.equals(oldEnd, newEnd)
                || oldSlotMinutes != newSlotMinutes;

        validateWindow(newDate, newStart, newEnd, newSlotMinutes, newCapacity);

        if (timeChanged) {
            if (hasUsed(oldSlots) || hasReservationHistory(oldSlots)) {
                throw new BusinessException(ErrorCode.IN_USE,
                        "该排班已有预约占用，不能修改日期/时间/拆分步长，可将状态改为无效");
            }
            ensureNotPast(newDate);
            ensureNoOverlap(schedule.getResourceId(), newDate, newStart, newEnd, schedule.getId());
        }

        int maxUsed = oldSlots.stream()
                .mapToInt(slot -> slot.getUsed() == null ? 0 : slot.getUsed())
                .max().orElse(0);
        if (newCapacity < maxUsed) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "容量不能小于已占用数 " + maxUsed);
        }

        boolean changed = timeChanged
                || oldCapacity != newCapacity
                || oldStatus != newStatus;
        if (!changed) {
            return toResponse(schedule, resource, oldSlots);
        }

        schedule.setScheduleDate(newDate);
        schedule.setStartTime(newStart);
        schedule.setEndTime(newEnd);
        schedule.setCapacity(newCapacity);
        schedule.setSlotMinutes(newSlotMinutes);
        schedule.setStatus(newStatus);
        scheduleMapper.updateById(schedule);

        if (timeChanged) {
            slotInventoryMapper.deleteWindow(schedule.getResourceId(), oldDate, oldStart, oldEnd);
            generateSlots(schedule);
        } else {
            if (oldCapacity != newCapacity) {
                slotInventoryMapper.updateTotalWindow(schedule.getResourceId(),
                        newDate, newStart, newEnd, newCapacity);
            }
            if (oldStatus != newStatus) {
                slotInventoryMapper.updateStatusWindow(schedule.getResourceId(),
                        newDate, newStart, newEnd, newStatus);
            }
        }

        Schedule saved = scheduleMapper.selectById(id);
        return toResponse(saved, resource, loadWindowSlots(saved));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Schedule schedule = findSchedule(id);
        if (!isDeletable(schedule)) {
            throw new BusinessException(ErrorCode.IN_USE,
                    "该排班已有预约占用，无法删除，可将状态改为无效");
        }
        removeSchedule(schedule);
    }

    @Override
    @Transactional
    public ScheduleBatchDeleteResponse batchDelete(List<Long> ids) {
        long merchantId = loginContext.merchantId();
        List<ScheduleBatchDeleteResponse.SkippedItem> skipped = new ArrayList<>();
        int deleted = 0;

        for (Long id : ids.stream().filter(Objects::nonNull).distinct().toList()) {
            Schedule schedule = scheduleMapper.selectById(id);
            if (schedule == null
                    || !Objects.equals(schedule.getMerchantId(), merchantId)) {
                skipped.add(ScheduleBatchDeleteResponse.SkippedItem.builder()
                        .id(id)
                        .reason("排班不存在")
                        .build());
                continue;
            }
            if (!isDeletable(schedule)) {
                skipped.add(ScheduleBatchDeleteResponse.SkippedItem.builder()
                        .id(id)
                        .reason("已有预约占用，无法删除")
                        .build());
                continue;
            }
            removeSchedule(schedule);
            deleted++;
        }

        return ScheduleBatchDeleteResponse.builder()
                .deletedCount(deleted)
                .skippedCount(skipped.size())
                .skipped(skipped)
                .build();
    }

    @Override
    public List<SlotInventoryResponse> slots(Long id) {
        Schedule schedule = findSchedule(id);
        Resource resource = findResource(schedule.getResourceId(), schedule.getMerchantId());
        return loadWindowSlots(schedule).stream()
                .map(slot -> toSlotResponse(slot, resource))
                .toList();
    }

    @Override
    public List<CalendarDayResponse> calendar(LocalDate startDate, LocalDate endDate, Long resourceId) {
        long merchantId = loginContext.merchantId();
        LocalDate start = startDate == null ? LocalDate.now(zoneId) : startDate;
        LocalDate end = endDate == null ? start.plusDays(6) : endDate;
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_CALENDAR_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "日历一次最多查询 " + MAX_CALENDAR_DAYS + " 天");
        }
        if (resourceId != null) {
            findResource(resourceId, merchantId);
        }

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getMerchantId, merchantId)
                .between(Schedule::getScheduleDate, start, end);
        if (resourceId != null) {
            wrapper.eq(Schedule::getResourceId, resourceId);
        }
        wrapper.orderByAsc(Schedule::getScheduleDate).orderByAsc(Schedule::getStartTime);

        List<Schedule> schedules = scheduleMapper.selectList(wrapper);
        Map<Long, Resource> resourceMap = loadResources(schedules);
        Map<ResourceDateKey, List<SlotInventory>> slotMap = loadSlotsForSchedules(schedules);
        Map<LocalDate, List<Schedule>> schedulesByDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getScheduleDate));

        List<CalendarDayResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<Schedule> daySchedules = schedulesByDate.getOrDefault(date, List.of());
            List<ScheduleResponse> scheduleResponses = daySchedules.stream()
                    .map(schedule -> toResponse(schedule,
                            resourceMap.get(schedule.getResourceId()),
                            windowSlots(schedule, slotMap)))
                    .toList();

            int slotCount = scheduleResponses.stream()
                    .mapToInt(ScheduleResponse::getSlotCount).sum();
            int totalCapacity = scheduleResponses.stream()
                    .mapToInt(ScheduleResponse::getTotalCapacity).sum();
            int usedCount = scheduleResponses.stream()
                    .mapToInt(ScheduleResponse::getUsedCount).sum();
            int availableCount = scheduleResponses.stream()
                    .mapToInt(ScheduleResponse::getAvailableCount).sum();
            int lockedCount = scheduleResponses.stream()
                    .mapToInt(ScheduleResponse::getLockedCount).sum();

            result.add(CalendarDayResponse.builder()
                    .date(date)
                    .scheduleCount(scheduleResponses.size())
                    .slotCount(slotCount)
                    .totalCapacity(totalCapacity)
                    .usedCount(usedCount)
                    .availableCount(availableCount)
                    .lockedCount(lockedCount)
                    .schedules(scheduleResponses)
                    .build());
        }
        return result;
    }

    /** 按 id 查排班，并校验属于当前商户 */
    private Schedule findSchedule(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null
                || !Objects.equals(schedule.getMerchantId(), loginContext.merchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "排班不存在");
        }
        return schedule;
    }

    /** 按 id 查资源，并校验属于指定商户 */
    private Resource findResource(Long resourceId, long merchantId) {
        if (resourceId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !Objects.equals(resource.getMerchantId(), merchantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    /** 新增排班时资源必须是当前商户的启用资源 */
    private Resource validateResourceForSchedule(Long resourceId, long merchantId) {
        Resource resource = findResource(resourceId, merchantId);
        if (resource.getStatus() == null || resource.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源已禁用，不能新增排班");
        }
        return resource;
    }

    /** 校验排班窗口：开始早于结束、容量合法、时长是步长整数倍 */
    private void validateWindow(LocalDate date, LocalTime start, LocalTime end,
                                int slotMinutes, int capacity) {
        if (date == null || start == null || end == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排班日期和时间不能为空");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
        if (slotMinutes < MIN_SLOT_MINUTES || slotMinutes > MAX_SLOT_MINUTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "拆分步长必须在 " + MIN_SLOT_MINUTES + "-" + MAX_SLOT_MINUTES + " 分钟之间");
        }
        if (capacity < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "容量至少为 1");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0 || minutes % slotMinutes != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "排班时长必须是拆分步长 " + slotMinutes + " 分钟的整数倍");
        }
    }

    private void ensureNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(zoneId))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排班日期不能早于今天");
        }
    }

    private void ensureNoOverlap(Long resourceId, LocalDate date, LocalTime start,
                                 LocalTime end, Long excludeId) {
        if (overlapCount(resourceId, date, start, end, excludeId) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA,
                    "该资源在所选日期/时间已有排班，时间窗口不能重叠");
        }
    }

    private long overlapCount(Long resourceId, LocalDate date, LocalTime start,
                              LocalTime end, Long excludeId) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getResourceId, resourceId)
                .eq(Schedule::getScheduleDate, date)
                .lt(Schedule::getStartTime, end)
                .gt(Schedule::getEndTime, start);
        if (excludeId != null) {
            wrapper.ne(Schedule::getId, excludeId);
        }
        Long count = scheduleMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    /** 按排班窗口生成库存；排班停用时生成的库存直接锁定 */
    private void generateSlots(Schedule schedule) {
        int step = schedule.getSlotMinutes();
        int slotStatus = schedule.getStatus() != null && schedule.getStatus() == STATUS_ACTIVE
                ? SLOT_SELLABLE : SLOT_LOCKED;
        List<SlotInventory> slots = new ArrayList<>();
        LocalTime cursor = schedule.getStartTime();

        while (true) {
            LocalTime slotEnd = cursor.plusMinutes(step);
            // slotEnd 回绕到 00:00 或超过窗口结束时间时停止
            if (!slotEnd.isAfter(cursor) || slotEnd.isAfter(schedule.getEndTime())) {
                break;
            }
            SlotInventory slot = new SlotInventory();
            slot.setMerchantId(schedule.getMerchantId());
            slot.setResourceId(schedule.getResourceId());
            slot.setSlotDate(schedule.getScheduleDate());
            slot.setStartTime(cursor);
            slot.setEndTime(slotEnd);
            slot.setTotal(schedule.getCapacity());
            slot.setUsed(0);
            slot.setVersion(0);
            slot.setStatus(slotStatus);
            slots.add(slot);
            cursor = slotEnd;
        }

        if (!slots.isEmpty()) {
            slotInventoryMapper.upsertBatch(slots);
        }
    }

    private List<SlotInventory> loadWindowSlots(Schedule schedule) {
        return slotInventoryMapper.selectWindow(schedule.getResourceId(), schedule.getScheduleDate(),
                schedule.getStartTime(), schedule.getEndTime());
    }

    private boolean hasUsed(List<SlotInventory> slots) {
        return slots.stream().anyMatch(slot -> slot.getUsed() != null && slot.getUsed() > 0);
    }

    private boolean hasReservationHistory(List<SlotInventory> slots) {
        List<Long> slotIds = slots.stream()
                .map(SlotInventory::getId)
                .filter(Objects::nonNull)
                .toList();
        return !slotIds.isEmpty() && reservationMapper.countBySlotIds(slotIds) > 0;
    }

    /** 窗口内没有占用且没有预约历史才允许删除 */
    private boolean isDeletable(Schedule schedule) {
        List<SlotInventory> slots = loadWindowSlots(schedule);
        return !hasUsed(slots) && !hasReservationHistory(slots);
    }

    /** 先删窗口库存再删排班 */
    private void removeSchedule(Schedule schedule) {
        slotInventoryMapper.deleteWindow(schedule.getResourceId(), schedule.getScheduleDate(),
                schedule.getStartTime(), schedule.getEndTime());
        scheduleMapper.deleteById(schedule.getId());
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
    }

    private Set<Integer> normalizeWeekdays(List<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> result = new HashSet<>();
        for (Integer weekday : weekdays) {
            if (weekday == null || weekday < 1 || weekday > 7) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "星期只能是 1-7");
            }
            result.add(weekday);
        }
        return result;
    }

    private Map<Long, Resource> loadResources(List<Schedule> schedules) {
        List<Long> resourceIds = schedules.stream()
                .map(Schedule::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return resourceMapper.selectBatchIds(resourceIds).stream()
                .collect(Collectors.toMap(Resource::getId, resource -> resource));
    }

    /**
     * 一次性查出当前页排班涉及的库存，避免每条排班单独查一次（N+1）。
     */
    private Map<ResourceDateKey, List<SlotInventory>> loadSlotsForSchedules(List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> resourceIds = schedules.stream()
                .map(Schedule::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        LocalDate minDate = schedules.stream()
                .map(Schedule::getScheduleDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate maxDate = schedules.stream()
                .map(Schedule::getScheduleDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (resourceIds.isEmpty() || minDate == null || maxDate == null) {
            return Collections.emptyMap();
        }

        List<SlotInventory> slots = slotInventoryMapper.selectList(
                new LambdaQueryWrapper<SlotInventory>()
                        .in(SlotInventory::getResourceId, resourceIds)
                        .between(SlotInventory::getSlotDate, minDate, maxDate));

        return slots.stream().collect(Collectors.groupingBy(
                slot -> new ResourceDateKey(slot.getResourceId(), slot.getSlotDate())));
    }

    private List<SlotInventory> windowSlots(Schedule schedule,
                                            Map<ResourceDateKey, List<SlotInventory>> slotMap) {
        List<SlotInventory> slots = slotMap.get(
                new ResourceDateKey(schedule.getResourceId(), schedule.getScheduleDate()));
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .filter(slot -> !slot.getStartTime().isBefore(schedule.getStartTime())
                        && slot.getStartTime().isBefore(schedule.getEndTime()))
                .toList();
    }

    private ScheduleResponse toResponse(Schedule schedule, Resource resource,
                                        List<SlotInventory> slots) {
        int totalCapacity = slots.stream()
                .mapToInt(slot -> slot.getTotal() == null ? 0 : slot.getTotal()).sum();
        int usedCount = slots.stream()
                .mapToInt(slot -> slot.getUsed() == null ? 0 : slot.getUsed()).sum();
        int availableCount = slots.stream()
                .mapToInt(slot -> {
                    if (slot.getStatus() == null || slot.getStatus() != SLOT_SELLABLE) {
                        return 0;
                    }
                    int total = slot.getTotal() == null ? 0 : slot.getTotal();
                    int used = slot.getUsed() == null ? 0 : slot.getUsed();
                    return Math.max(total - used, 0);
                }).sum();
        int lockedCount = (int) slots.stream()
                .filter(slot -> slot.getStatus() != null && slot.getStatus() == SLOT_LOCKED)
                .count();

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .merchantId(schedule.getMerchantId())
                .resourceId(schedule.getResourceId())
                .resourceName(resource == null ? null : resource.getName())
                .resourceType(resource == null ? null : resource.getType())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .capacity(schedule.getCapacity())
                .slotMinutes(schedule.getSlotMinutes() == null
                        ? DEFAULT_SLOT_MINUTES : schedule.getSlotMinutes())
                .status(schedule.getStatus())
                .slotCount(slots.size())
                .totalCapacity(totalCapacity)
                .usedCount(usedCount)
                .availableCount(availableCount)
                .lockedCount(lockedCount)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    private SlotInventoryResponse toSlotResponse(SlotInventory slot, Resource resource) {
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

    /** 库存按「资源 + 日期」分组的键 */
    private record ResourceDateKey(Long resourceId, LocalDate date) {
    }
}
