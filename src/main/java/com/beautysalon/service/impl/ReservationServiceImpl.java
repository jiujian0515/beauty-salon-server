package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.CustomerContext;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.AuditLogMapper;
import com.beautysalon.mapper.DepositTransactionMapper;
import com.beautysalon.mapper.ReservationMapper;
import com.beautysalon.mapper.ResourceMapper;
import com.beautysalon.mapper.ServiceItemMapper;
import com.beautysalon.mapper.SlotInventoryMapper;
import com.beautysalon.model.dto.ReservationCancelResponse;
import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;
import com.beautysalon.model.dto.ReservationResponse;
import com.beautysalon.model.entity.AuditLog;
import com.beautysalon.model.entity.Customer;
import com.beautysalon.model.entity.DepositTransaction;
import com.beautysalon.model.entity.Reservation;
import com.beautysalon.model.entity.Resource;
import com.beautysalon.model.entity.ServiceItem;
import com.beautysalon.model.entity.SlotInventory;
import com.beautysalon.service.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 预约服务实现
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final int SERVICE_ON = 1;
    private static final int SLOT_SELLABLE = 1;
    private static final int RESOURCE_ENABLED = 1;

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REFUNDED = "REFUNDED";
    private static final String TX_PENDING = "PENDING";
    private static final String TX_SUCCESS = "SUCCESS";
    private static final String TX_REFUNDED = "REFUNDED";
    private static final String CHANNEL_MOCK = "MOCK";
    private static final String ACTION_CANCEL_RESERVATION = "CANCEL_RESERVATION";
    private static final String TARGET_RESERVATION = "RESERVATION";

    /** 预约单允许的状态（用于「我的预约」筛选参数校验） */
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING_PAYMENT", "CONFIRMED", "COMPLETED",
            "CANCELLED", "EXPIRED", "REFUNDING", "REFUNDED");

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReservationMapper reservationMapper;
    private final DepositTransactionMapper depositTransactionMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final SlotInventoryMapper slotInventoryMapper;
    private final ResourceMapper resourceMapper;
    private final AuditLogMapper auditLogMapper;
    private final CustomerContext customerContext;
    private final ZoneId zoneId;
    private final int leadHours;
    private final int cancelAheadHours;

    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  DepositTransactionMapper depositTransactionMapper,
                                  ServiceItemMapper serviceItemMapper,
                                  SlotInventoryMapper slotInventoryMapper,
                                  ResourceMapper resourceMapper,
                                  AuditLogMapper auditLogMapper,
                                  CustomerContext customerContext,
                                  @Value("${app.timezone:Asia/Shanghai}") String timezone,
                                  @Value("${app.booking.lead-hours:24}") int leadHours,
                                  @Value("${app.payment.cancel-ahead-hours:24}") int cancelAheadHours) {
        this.reservationMapper = reservationMapper;
        this.depositTransactionMapper = depositTransactionMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.slotInventoryMapper = slotInventoryMapper;
        this.resourceMapper = resourceMapper;
        this.auditLogMapper = auditLogMapper;
        this.customerContext = customerContext;
        this.zoneId = ZoneId.of(timezone);
        this.leadHours = leadHours;
        this.cancelAheadHours = cancelAheadHours;
    }

    @Override
    @Transactional
    public ReservationCreateResponse create(ReservationCreateRequest request) {
        Customer customer = customerContext.currentCustomer();
        boolean course = Boolean.TRUE.equals(request.getIsCourse());

        ServiceItem serviceItem = serviceItemMapper.selectById(request.getServiceItemId());
        if (serviceItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "服务项目不存在");
        }
        if (!Objects.equals(serviceItem.getStatus(), SERVICE_ON)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务项目已下架");
        }

        SlotInventory slot = slotInventoryMapper.selectById(request.getSlotId());
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "时段不存在");
        }
        if (!Objects.equals(slot.getStatus(), SLOT_SELLABLE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时段已锁定，请选择其他时段");
        }
        if (slot.getUsed() != null && slot.getTotal() != null
                && slot.getUsed() >= slot.getTotal()) {
            throw new BusinessException(ErrorCode.SLOT_FULL);
        }
        if (!Objects.equals(serviceItem.getMerchantId(), slot.getMerchantId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务项目与时段不属于同一商户");
        }

        Resource resource = resourceMapper.selectById(slot.getResourceId());
        if (resource == null || !Objects.equals(resource.getStatus(), RESOURCE_ENABLED)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约资源不存在或已禁用");
        }
        if (!Objects.equals(resource.getMerchantId(), slot.getMerchantId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源不属于该商户");
        }

        LocalDateTime appointmentTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        LocalDateTime earliestStart = LocalDateTime.now(zoneId).plusHours(leadHours);
        if (appointmentTime.isBefore(earliestStart)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "预约时间需至少提前 " + leadHours + " 小时");
        }

        // 疗程预约免定金；普通预约取服务项目配置的定金，定金为 0 时同样无需支付。
        long depositCents = course
                ? 0L
                : (serviceItem.getDepositCents() == null ? 0L : serviceItem.getDepositCents());
        boolean paymentRequired = !course && depositCents > 0;

        // 原子占用库存：影响行数为 0 说明已被抢完或已锁定。
        int occupied = slotInventoryMapper.occupyOne(slot.getId());
        if (occupied == 0) {
            throw new BusinessException(ErrorCode.SLOT_FULL);
        }

        Reservation reservation = new Reservation();
        reservation.setReservationNo(generateNo("R"));
        reservation.setMerchantId(slot.getMerchantId());
        reservation.setCustomerId(customer.getId());
        reservation.setServiceItemId(serviceItem.getId());
        reservation.setResourceId(slot.getResourceId());
        reservation.setSlotInventoryId(slot.getId());
        reservation.setAppointmentTime(appointmentTime);
        reservation.setDepositCents(depositCents);
        reservation.setIsCourse(course ? 1 : 0);
        reservation.setStatus(paymentRequired ? STATUS_PENDING_PAYMENT : STATUS_CONFIRMED);
        reservation.setVersion(0);
        reservationMapper.insert(reservation);

        String orderNo = null;
        Map<String, Object> paymentParams = null;
        if (paymentRequired) {
            DepositTransaction transaction = new DepositTransaction();
            transaction.setMerchantId(slot.getMerchantId());
            transaction.setReservationId(reservation.getId());
            transaction.setOrderNo(generateNo("P"));
            transaction.setOutTradeNo(generateNo("T"));
            transaction.setChannel(CHANNEL_MOCK);
            transaction.setAmountCents(depositCents);
            transaction.setStatus(TX_PENDING);
            depositTransactionMapper.insert(transaction);

            orderNo = transaction.getOrderNo();
            paymentParams = new LinkedHashMap<>();
            paymentParams.put("channel", CHANNEL_MOCK);
            paymentParams.put("outTradeNo", transaction.getOutTradeNo());
            paymentParams.put("amountCents", depositCents);
            paymentParams.put("mockPay", true);
        }

        return ReservationCreateResponse.builder()
                .reservationId(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .status(reservation.getStatus())
                .isCourse(course)
                .depositCents(depositCents)
                .paymentRequired(paymentRequired)
                .orderNo(orderNo)
                .appointmentTime(appointmentTime)
                .merchantId(slot.getMerchantId())
                .resourceId(slot.getResourceId())
                .resourceName(resource.getName())
                .slotId(slot.getId())
                .paymentParams(paymentParams)
                .build();
    }

    @Override
    public PageResult<ReservationResponse> mine(int page, int size, String status) {
        // 与创建/支付接口一致：校验顾客存在且启用，禁用账号无法查看预约
        long customerId = customerContext.currentCustomer().getId();
        String normalizedStatus = normalizeStatus(status);

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        // 只查当前登录顾客自己的预约，防止越权
        wrapper.eq(Reservation::getCustomerId, customerId);
        if (normalizedStatus != null) {
            wrapper.eq(Reservation::getStatus, normalizedStatus);
        }
        wrapper.orderByDesc(Reservation::getAppointmentTime)
                .orderByDesc(Reservation::getId);

        Page<Reservation> result = reservationMapper.selectPage(new Page<>(page, size), wrapper);
        List<Reservation> reservations = result.getRecords();

        // 批量加载关联数据，避免列表接口 N+1 查询
        Map<Long, ServiceItem> serviceItemMap = loadServiceItems(reservations);
        Map<Long, Resource> resourceMap = loadResources(reservations);
        Map<Long, String> outTradeNoMap = loadPendingOutTradeNos(reservations);

        List<ReservationResponse> records = reservations.stream()
                .map(reservation -> toResponse(
                        reservation,
                        serviceItemMap.get(reservation.getServiceItemId()),
                        resourceMap.get(reservation.getResourceId()),
                        outTradeNoMap.get(reservation.getId())))
                .toList();

        return PageResult.of(result, records);
    }

    /** 归一化并校验状态筛选参数；空值表示不筛选 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约状态不合法");
        }
        return normalized;
    }

    /** 批量查当前页涉及的服务项目 */
    private Map<Long, ServiceItem> loadServiceItems(List<Reservation> reservations) {
        List<Long> ids = reservations.stream()
                .map(Reservation::getServiceItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceItemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ServiceItem::getId, item -> item));
    }

    /** 批量查当前页涉及的资源 */
    private Map<Long, Resource> loadResources(List<Reservation> reservations) {
        List<Long> ids = reservations.stream()
                .map(Reservation::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return resourceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Resource::getId, resource -> resource));
    }

    /** 批量查当前页待支付预约对应的平台交易号（用于列表「继续支付」） */
    private Map<Long, String> loadPendingOutTradeNos(List<Reservation> reservations) {
        List<Long> ids = reservations.stream()
                .filter(item -> STATUS_PENDING_PAYMENT.equals(item.getStatus()))
                .map(Reservation::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<DepositTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DepositTransaction::getReservationId, ids)
                .eq(DepositTransaction::getStatus, TX_PENDING);
        return depositTransactionMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(
                        DepositTransaction::getReservationId,
                        DepositTransaction::getOutTradeNo,
                        (first, second) -> first));
    }

    private ReservationResponse toResponse(Reservation reservation,
                                           ServiceItem serviceItem,
                                           Resource resource,
                                           String outTradeNo) {
        return ReservationResponse.builder()
                .reservationId(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .status(reservation.getStatus())
                .isCourse(reservation.getIsCourse() != null && reservation.getIsCourse() == 1)
                .depositCents(reservation.getDepositCents())
                .appointmentTime(reservation.getAppointmentTime())
                .merchantId(reservation.getMerchantId())
                .serviceItemId(reservation.getServiceItemId())
                .serviceItemName(serviceItem == null ? null : serviceItem.getName())
                .serviceItemDurationMinutes(serviceItem == null ? null : serviceItem.getDurationMinutes())
                .resourceId(reservation.getResourceId())
                .resourceName(resource == null ? null : resource.getName())
                .resourceType(resource == null ? null : resource.getType())
                .outTradeNo(outTradeNo)
                .paidAt(reservation.getPaidAt())
                .cancelledAt(reservation.getCancelledAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    /** 生成业务号：前缀 + 秒级时间 + 6 位随机数，长度 21，满足 VARCHAR(32)。 */
    @Override
    @Transactional
    public ReservationCancelResponse cancel(Long id) {
        Customer customer = customerContext.currentCustomer();
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约单不存在");
        }
        if (!customer.getId().equals(reservation.getCustomerId())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "无权取消该预约");
        }

        String status = reservation.getStatus();
        if (STATUS_PENDING_PAYMENT.equals(status)) {
            // 未支付：无需退款，直接取消并让待支付流水过期
            return cancelWithoutRefund(reservation, STATUS_PENDING_PAYMENT, true);
        }
        if (!STATUS_CONFIRMED.equals(status)) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "预约状态不允许取消");
        }

        // 已确认预约需至少提前 cancelAheadHours 小时取消
        LocalDateTime cancelDeadline = reservation.getAppointmentTime().minusHours(cancelAheadHours);
        if (LocalDateTime.now(zoneId).isAfter(cancelDeadline)) {
            throw new BusinessException(ErrorCode.CANCEL_TOO_LATE);
        }

        DepositTransaction transaction =
                depositTransactionMapper.selectByReservationId(reservation.getId());
        if (transaction != null && TX_SUCCESS.equals(transaction.getStatus())) {
            return refundAndCancel(reservation, transaction);
        }
        return cancelWithoutRefund(reservation, STATUS_CONFIRMED, false);
    }

    /**
     * 不涉及退款的取消：状态置为 CANCELLED 并释放时段库存
     */
    private ReservationCancelResponse cancelWithoutRefund(Reservation reservation,
                                                          String fromStatus,
                                                          boolean expirePendingTx) {
        int changed = reservationMapper.markCancelled(reservation.getId(), fromStatus);
        if (changed == 0) {
            // 并发场景下已被其他请求处理，返回最终状态
            return resolveFinalState(reservation.getId());
        }
        if (expirePendingTx) {
            depositTransactionMapper.markExpired(reservation.getId());
        }
        // used 已为 0 时影响行数为 0，不阻断取消
        slotInventoryMapper.releaseOne(reservation.getSlotInventoryId());
        writeCancelAuditLog(reservation, false, 0L);
        return buildCancelResponse(reservation.getId(), false, 0L);
    }

    /**
     * 已支付定金的取消：CONFIRMED → REFUNDING → REFUNDED，定金流水 SUCCESS → REFUNDED
     */
    private ReservationCancelResponse refundAndCancel(Reservation reservation,
                                                      DepositTransaction transaction) {
        if (reservationMapper.markRefunding(reservation.getId()) == 0) {
            return resolveFinalState(reservation.getId());
        }
        if (depositTransactionMapper.markRefunded(reservation.getId()) == 0) {
            DepositTransaction latest =
                    depositTransactionMapper.selectByReservationId(reservation.getId());
            if (latest == null || !TX_REFUNDED.equals(latest.getStatus())) {
                throw new BusinessException(ErrorCode.STATUS_INVALID, "定金状态不允许退款");
            }
        }
        if (reservationMapper.markRefunded(reservation.getId()) == 0) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "预约状态不允许取消");
        }
        slotInventoryMapper.releaseOne(reservation.getSlotInventoryId());
        long amount = transaction.getAmountCents() == null ? 0L : transaction.getAmountCents();
        writeCancelAuditLog(reservation, true, amount);
        return buildCancelResponse(reservation.getId(), true, amount);
    }

    /**
     * 状态流转未生效时，读取最新状态；已是终态则按幂等返回，否则报状态异常
     */
    private ReservationCancelResponse resolveFinalState(Long id) {
        Reservation latest = reservationMapper.selectById(id);
        if (latest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约单不存在");
        }
        if (STATUS_CANCELLED.equals(latest.getStatus())) {
            return buildCancelResponse(latest, false, 0L);
        }
        if (STATUS_REFUNDED.equals(latest.getStatus())) {
            long amount = latest.getDepositCents() == null ? 0L : latest.getDepositCents();
            return buildCancelResponse(latest, true, amount);
        }
        throw new BusinessException(ErrorCode.STATUS_INVALID, "预约状态不允许取消");
    }

    private ReservationCancelResponse buildCancelResponse(Long id, boolean refunded, long amount) {
        return buildCancelResponse(reservationMapper.selectById(id), refunded, amount);
    }

    private ReservationCancelResponse buildCancelResponse(Reservation reservation,
                                                          boolean refunded,
                                                          long amount) {
        return ReservationCancelResponse.builder()
                .reservationId(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .status(reservation.getStatus())
                .refunded(refunded)
                .refundAmountCents(amount)
                .cancelledAt(reservation.getCancelledAt())
                .build();
    }

    private void writeCancelAuditLog(Reservation reservation, boolean refunded, long amount) {
        AuditLog auditLog = new AuditLog();
        auditLog.setMerchantId(reservation.getMerchantId());
        auditLog.setActorId(reservation.getCustomerId());
        auditLog.setAction(ACTION_CANCEL_RESERVATION);
        auditLog.setTargetType(TARGET_RESERVATION);
        auditLog.setTargetId(reservation.getId());
        auditLog.setDetail(refunded
                ? "顾客取消预约并退款定金，amountCents=" + amount
                : "顾客取消预约（无需退款）");
        auditLogMapper.insert(auditLog);
    }

    private String generateNo(String prefix) {
        return prefix
                + LocalDateTime.now(zoneId).format(NO_TIME_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
