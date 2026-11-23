package com.beautysalon.controller;

import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.PageResult;
import com.beautysalon.common.Result;
import com.beautysalon.model.dto.ServiceItemResponse;
import com.beautysalon.service.ServiceItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端服务项目接口（公开，无需登录）
 *
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>作用</th></tr>
 *   <tr><td>GET</td><td>/api/v1/services</td><td>分页查询上架服务项目</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/services")
public class CustomerServiceItemController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ServiceItemService serviceItemService;

    public CustomerServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    /**
     * 分页查询上架服务项目
     *
     * <p>示例：GET /api/v1/services?page=1&size=10&keyword=清洁&merchantId=1&category=面部护理</p>
     * <p>不传 merchantId 时返回所有商户的上架服务；下架服务不会出现在顾客端。</p>
     */
    @GetMapping
    public Result<PageResult<ServiceItemResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String category) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页码必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每页条数必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return Result.ok(serviceItemService.customerPage(page, size, keyword, merchantId, category));
    }
}
