package com.beautysalon.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班实体（与数据库 schedule 表一一对应）
 *
 * <p>一条排班表示某个资源在某一天的一段可售时间窗口。
 * 保存排班时会按 {@link #slotMinutes} 把窗口拆成多条时段库存。</p>
 */
@Data
@TableName("schedule")
public class Schedule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商户 */
    private Long merchantId;

    /** 资源 id（STAFF 技师 / ROOM 床位） */
    private Long resourceId;

    /** 排班日期 */
    private LocalDate scheduleDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 容量（同一时段可同时服务的数量） */
    private Integer capacity;

    /** 库存拆分步长（分钟） */
    private Integer slotMinutes;

    /** 状态：1有效 0无效 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
