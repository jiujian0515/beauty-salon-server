package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.ServiceItemCreateRequest;
import com.beautysalon.model.dto.ServiceItemResponse;
import com.beautysalon.model.dto.ServiceItemUpdateRequest;

/**
 * 服务项目管理服务
 */
public interface ServiceItemService {

    /**
     * 分页查询服务项目
     *
     * @param category 可选，按分类精确筛选
     */
    PageResult<ServiceItemResponse> page(int page, int size, String keyword, Integer status, String category);

    /**
     * 顾客端分页查询上架服务项目（公开接口，无登录态）
     *
     * @param merchantId 可选，按商户过滤；为空时查询所有商户
     * @param category   可选，按分类精确筛选
     */
    PageResult<ServiceItemResponse> customerPage(int page, int size, String keyword, Long merchantId, String category);

    /** 查询服务项目详情 */
    ServiceItemResponse detail(Long id);

    /** 新增服务项目 */
    ServiceItemResponse create(ServiceItemCreateRequest request);

    /** 编辑服务项目（局部更新） */
    ServiceItemResponse update(Long id, ServiceItemUpdateRequest request);

    /** 删除服务项目 */
    void delete(Long id);
}
