package com.beautysalon.service;

import com.beautysalon.common.PageResult;
import com.beautysalon.model.dto.ResourceCreateRequest;
import com.beautysalon.model.dto.ResourceResponse;
import com.beautysalon.model.dto.ResourceUpdateRequest;
import com.beautysalon.model.dto.StaffCandidateResponse;

import java.util.List;

/**
 * 资源管理服务（技师 STAFF / 床位 ROOM）
 */
public interface ResourceService {

    /** 分页查询资源 */
    PageResult<ResourceResponse> page(int page, int size, String keyword, String type, Integer status);

    /** 查询资源详情 */
    ResourceResponse detail(Long id);

    /** 新增资源 */
    ResourceResponse create(ResourceCreateRequest request);

    /** 编辑资源（局部更新） */
    ResourceResponse update(Long id, ResourceUpdateRequest request);

    /** 删除资源（被排班/库存/预约引用时不允许删除） */
    void delete(Long id);

    /**
     * 查询技师候选用户
     *
     * @param resourceId 编辑资源时传当前资源 id，用于把「已绑定到该资源」的用户保留在候选列表中
     */
    List<StaffCandidateResponse> staffCandidates(Long resourceId);
}
