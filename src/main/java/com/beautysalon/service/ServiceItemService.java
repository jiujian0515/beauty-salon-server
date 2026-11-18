package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.ServiceItemCreateRequest;
import com.beautysalon.model.dto.ServiceItemResponse;
import com.beautysalon.model.dto.ServiceItemUpdateRequest;

/**
 * 服务项目管理服务
 */
public interface ServiceItemService {

    /** 分页查询服务项目 */
    PageResult<ServiceItemResponse> page(int page, int size, String keyword, Integer status);

    /** 查询服务项目详情 */
    ServiceItemResponse detail(Long id);

    /** 新增服务项目 */
    ServiceItemResponse create(ServiceItemCreateRequest request);

    /** 编辑服务项目（局部更新） */
    ServiceItemResponse update(Long id, ServiceItemUpdateRequest request);

    /** 删除服务项目 */
    void delete(Long id);
}
