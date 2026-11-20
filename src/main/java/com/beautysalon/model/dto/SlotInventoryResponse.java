package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 时段库存返回体
 */
@Data
@Builder
public class SlotInventoryResponse {

    /** 时段 id */
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id */
    private Long resourceId;

    /** 资源名称 */
    private String resourceName;

    /** 时段日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate slotDate;

    /** 开始时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 总容量 */
    private Integer total;

    /** 已占用数 */
    private Integer used;

    /** 剩余可售数 */
    private Integer available;

    /** 乐观锁版本号 */
    private Integer version;

    /** 状态：1可售 0锁定 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
