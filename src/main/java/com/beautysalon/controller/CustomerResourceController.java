package com.beautysalon.controller;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.CustomerResourceResponse;
import com.beautysalon.service.ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端资源（美容师）接口（公开，无需登录）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/resources</td><td>分页查询启用的美容师</td></tr>
 * </table>
 *
 * <p>顾客选好服务项目和日期后，可先查询美容师列表，再把返回的 id 作为
 * {@code resourceId} 传给 {@code GET /api/v1/slots} 查询该美容师的可售时段。</p>
 */
@RestController
@RequestMapping("/api/v1/resources")
public class CustomerResourceController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ResourceService resourceService;

    public CustomerResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 分页查询启用的资源（默认只查美容师 STAFF）
     *
     * <p>示例：GET /api/v1/resources?page=1&size=10&keyword=刘&merchantId=1&type=STAFF</p>
     * <ul>
     *   <li>{@code keyword}：可选，按资源名称模糊匹配</li>
     *   <li>{@code merchantId}：可选，不传时返回所有商户的资源</li>
     *   <li>{@code type}：可选，STAFF（默认，美容师）/ ROOM（床位）</li>
     * </ul>
     * <p>禁用资源不会出现在顾客端。</p>
     */
    @GetMapping
    public Result<PageResult<CustomerResourceResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String type) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页码必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return Result.ok(resourceService.customerPage(page, size, keyword, merchantId, type));
    }
}
