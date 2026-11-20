package com.beautysalon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beautysalon.model.entity.SlotInventory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 时段库存 Mapper（数据访问层）
 *
 * <p>排班窗口内的库存用「resource_id + slot_date + start_time 区间」定位，
 * 因为 slot_inventory 表本身没有 schedule_id 字段。</p>
 */
@Mapper
public interface SlotInventoryMapper extends BaseMapper<SlotInventory> {

    /** 查询某条排班窗口内的库存，按开始时间升序 */
    @Select("SELECT * FROM slot_inventory "
            + "WHERE resource_id = #{resourceId} AND slot_date = #{slotDate} "
            + "AND start_time >= #{startTime} AND start_time < #{endTime} "
            + "ORDER BY start_time")
    List<SlotInventory> selectWindow(@Param("resourceId") Long resourceId,
                                     @Param("slotDate") LocalDate slotDate,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);

    /** 删除某条排班窗口内的库存 */
    @Delete("DELETE FROM slot_inventory "
            + "WHERE resource_id = #{resourceId} AND slot_date = #{slotDate} "
            + "AND start_time >= #{startTime} AND start_time < #{endTime}")
    int deleteWindow(@Param("resourceId") Long resourceId,
                     @Param("slotDate") LocalDate slotDate,
                     @Param("startTime") LocalTime startTime,
                     @Param("endTime") LocalTime endTime);

    /** 批量更新窗口内库存的总容量 */
    @Update("UPDATE slot_inventory SET total = #{total} "
            + "WHERE resource_id = #{resourceId} AND slot_date = #{slotDate} "
            + "AND start_time >= #{startTime} AND start_time < #{endTime}")
    int updateTotalWindow(@Param("resourceId") Long resourceId,
                          @Param("slotDate") LocalDate slotDate,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("total") Integer total);

    /** 批量更新窗口内库存的可售状态 */
    @Update("UPDATE slot_inventory SET status = #{status} "
            + "WHERE resource_id = #{resourceId} AND slot_date = #{slotDate} "
            + "AND start_time >= #{startTime} AND start_time < #{endTime}")
    int updateStatusWindow(@Param("resourceId") Long resourceId,
                           @Param("slotDate") LocalDate slotDate,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime,
                           @Param("status") Integer status);

    /** 按 id 更新单个时段的可售状态（不覆盖 used/version） */
    @Update("UPDATE slot_inventory SET status = #{status} WHERE id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 批量插入库存；唯一键冲突时只更新结束时间/容量/状态，保留已占用数。
     *
     * <p>用于「保存排班 → 生成库存」的幂等写入。</p>
     */
    @Insert("""
            <script>
            INSERT INTO slot_inventory
              (merchant_id, resource_id, slot_date, start_time, end_time, total, used, version, status)
            VALUES
            <foreach collection="list" item="item" separator=",">
              (#{item.merchantId}, #{item.resourceId}, #{item.slotDate}, #{item.startTime},
               #{item.endTime}, #{item.total}, #{item.used}, #{item.version}, #{item.status})
            </foreach>
            ON DUPLICATE KEY UPDATE
              end_time = VALUES(end_time),
              total = VALUES(total),
              status = VALUES(status)
            </script>
            """)
    int upsertBatch(@Param("list") List<SlotInventory> list);
}
