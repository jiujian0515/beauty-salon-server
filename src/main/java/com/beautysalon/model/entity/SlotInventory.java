package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 时段库存实体（与数据库 slot_inventory 表一一对应）
 *
 * <p>每条记录是一个可预约的具体时段，例如 09:00-10:00。
 * {@code total} 是总容量，{@code used} 是已占用数，{@code version} 用于乐观锁防超卖。</p>
 */
@Data
@TableName("slot_inventory")
public class SlotInventory {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id */
    private Long resourceId;

    /** 时段日期 */
    private LocalDate slotDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 总容量 */
    private Integer total;

    /** 已占用数 */
    private Integer used;

    /** 乐观锁版本号 */
    private Integer version;

    /** 状态：1可售 0锁定 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
