package com.beautysalon.service.impl;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.CustomerContext;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.mapper.DepositTransactionMapper;
import com.beautysalon.mapper.ReservationMapper;
import com.beautysalon.mapper.ResourceMapper;
import com.beautysalon.mapper.ServiceItemMapper;
import com.beautysalon.mapper.SlotInventoryMapper;
import com.beautysalon.model.dto.ReservationCreateRequest;
import com.beautysalon.model.dto.ReservationCreateResponse;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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
    private static final String TX_PENDING = "PENDING";
    private static final String CHANNEL_MOCK = "MOCK";

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReservationMapper reservationMapper;
    private final DepositTransactionMapper depositTransactionMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final SlotInventoryMapper slotInventoryMapper;
    private final ResourceMapper resourceMapper;
    private final CustomerContext customerContext;
    private final ZoneId zoneId;
    private final int leadHours;

    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  DepositTransactionMapper depositTransactionMapper,
                                  ServiceItemMapper serviceItemMapper,
                                  SlotInventoryMapper slotInventoryMapper,
                                  ResourceMapper resourceMapper,
                                  CustomerContext customerContext,
                                  @Value("${app.timezone:Asia/Shanghai}") String timezone,
                                  @Value("${app.booking.lead-hours:24}") int leadHours) {
        this.reservationMapper = reservationMapper;
        this.depositTransactionMapper = depositTransactionMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.slotInventoryMapper = slotInventoryMapper;
        this.resourceMapper = resourceMapper;
        this.customerContext = customerContext;
        this.zoneId = ZoneId.of(timezone);
        this.leadHours = leadHours;
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

    /** 生成业务号：前缀 + 秒级时间 + 6 位随机数，长度 21，满足 VARCHAR(32)。 */
    private String generateNo(String prefix) {
        return prefix
                + LocalDateTime.now(zoneId).format(NO_TIME_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
