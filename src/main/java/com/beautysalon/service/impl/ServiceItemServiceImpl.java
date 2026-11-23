package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.ServiceItemMapper;
import com.beautysalon.model.dto.ServiceItemCreateRequest;
import com.beautysalon.model.dto.ServiceItemResponse;
import com.beautysalon.model.dto.ServiceItemUpdateRequest;
import com.beautysalon.model.entity.ServiceItem;
import com.beautysalon.service.ServiceItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 服务项目管理实现（商户数据隔离）
 */
@Service
public class ServiceItemServiceImpl implements ServiceItemService {

    private static final int SERVICE_ITEM_ON_SHELF = 1;

    private final ServiceItemMapper serviceItemMapper;
    private final LoginContext loginContext;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper,
                                  LoginContext loginContext) {
        this.serviceItemMapper = serviceItemMapper;
        this.loginContext = loginContext;
    }

    @Override
    public PageResult<ServiceItemResponse> page(int page, int size, String keyword, Integer status,
                                                String category) {
        long merchantId = loginContext.merchantId();

        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceItem::getMerchantId, merchantId);

        if (status != null) {
            wrapper.eq(ServiceItem::getStatus, status);
        }

        if (StringUtils.hasText(category)) {
            wrapper.eq(ServiceItem::getCategory, category.trim());
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ServiceItem::getName, kw)
                    .or().like(ServiceItem::getDescription, kw));
        }

        wrapper.orderByDesc(ServiceItem::getId);

        Page<ServiceItem> result = serviceItemMapper.selectPage(new Page<>(page, size), wrapper);

        List<ServiceItemResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public PageResult<ServiceItemResponse> customerPage(int page, int size, String keyword, Long merchantId,
                                                        String category) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceItem::getStatus, SERVICE_ITEM_ON_SHELF);

        if (merchantId != null) {
            wrapper.eq(ServiceItem::getMerchantId, merchantId);
        }

        if (StringUtils.hasText(category)) {
            wrapper.eq(ServiceItem::getCategory, category.trim());
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ServiceItem::getName, kw)
                    .or().like(ServiceItem::getDescription, kw));
        }

        // 顾客端按创建顺序展示：先创建的服务项目排在前面
        wrapper.orderByAsc(ServiceItem::getId);

        Page<ServiceItem> result = serviceItemMapper.selectPage(new Page<>(page, size), wrapper);

        List<ServiceItemResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public ServiceItemResponse detail(Long id) {
        return toResponse(findServiceItem(id));
    }

    @Override
    public ServiceItemResponse create(ServiceItemCreateRequest request) {
        long merchantId = loginContext.merchantId();

        ServiceItem item = new ServiceItem();
        item.setMerchantId(merchantId);
        item.setName(request.getName());
        item.setCategory(request.getCategory().trim());
        item.setDescription(request.getDescription());
        item.setDurationMinutes(request.getDurationMinutes());
        item.setPriceCents(request.getPriceCents());
        item.setDepositCents(request.getDepositCents());
        item.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        serviceItemMapper.insert(item);
        return toResponse(item);
    }

    @Override
    public ServiceItemResponse update(Long id, ServiceItemUpdateRequest request) {
        ServiceItem item = findServiceItem(id);

        if (StringUtils.hasText(request.getName())) {
            item.setName(request.getName());
        }
        if (request.getCategory() != null) {
            if (!StringUtils.hasText(request.getCategory())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "服务分类不能为空");
            }
            item.setCategory(request.getCategory().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getDurationMinutes() != null) {
            item.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getPriceCents() != null) {
            item.setPriceCents(request.getPriceCents());
        }
        if (request.getDepositCents() != null) {
            item.setDepositCents(request.getDepositCents());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }

        serviceItemMapper.updateById(item);
        return toResponse(item);
    }

    @Override
    public void delete(Long id) {
        findServiceItem(id);
        serviceItemMapper.deleteById(id);
    }

    /** 按 id 查服务项目，并校验属于当前商户 */
    private ServiceItem findServiceItem(Long id) {
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null
                || !Objects.equals(item.getMerchantId(), loginContext.merchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "服务项目不存在");
        }
        return item;
    }

    private ServiceItemResponse toResponse(ServiceItem item) {
        return ServiceItemResponse.builder()
                .id(item.getId())
                .merchantId(item.getMerchantId())
                .name(item.getName())
                .category(item.getCategory())
                .description(item.getDescription())
                .durationMinutes(item.getDurationMinutes())
                .priceCents(item.getPriceCents())
                .depositCents(item.getDepositCents())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
