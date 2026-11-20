-- ============================================================
-- V6：排班拆分步长、时段库存状态，并为已有排班补齐库存
--
-- 背景：
--   V1 只建了 schedule / slot_inventory 两张表，并插入了一条演示库存。
--   排班功能上线后，需要按「排班窗口 + 固定步长」自动生成库存；
--   同时要支持把单个时段锁定为不可售。
--
-- 兼容策略：
--   - schedule.slot_minutes 默认 60，已有排班自动按 60 分钟拆分。
--   - slot_inventory.status 默认 1（可售），已有库存保持可售。
--   - 下面用递归 CTE 为已有排班补齐缺失库存；
--     ON DUPLICATE KEY UPDATE 只更新窗口结束时间/容量/状态，保留已有 used，
--     因此重复执行也不会覆盖预约占用。
-- ============================================================

ALTER TABLE schedule
  ADD COLUMN slot_minutes INT NOT NULL DEFAULT 60 COMMENT '库存拆分步长（分钟）' AFTER capacity,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
      ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE slot_inventory
  ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1可售 0锁定' AFTER version;

-- 为所有「启用」的已有排班补齐库存。
-- 例：09:00-18:00、步长 60 分钟 → 09:00-10:00 ... 17:00-18:00 共 9 条。
INSERT INTO slot_inventory
  (merchant_id, resource_id, slot_date, start_time, end_time, total, used, version, status)
WITH RECURSIVE slot_gen AS (
  SELECT
    s.merchant_id,
    s.resource_id,
    s.schedule_date,
    s.start_time AS slot_start,
    ADDTIME(s.start_time, SEC_TO_TIME(s.slot_minutes * 60)) AS slot_end,
    s.end_time,
    s.capacity,
    s.slot_minutes
  FROM schedule s
  WHERE s.status = 1
  UNION ALL
  SELECT
    g.merchant_id,
    g.resource_id,
    g.schedule_date,
    g.slot_end AS slot_start,
    ADDTIME(g.slot_end, SEC_TO_TIME(g.slot_minutes * 60)) AS slot_end,
    g.end_time,
    g.capacity,
    g.slot_minutes
  FROM slot_gen g
  WHERE g.slot_end < g.end_time
)
SELECT
  merchant_id,
  resource_id,
  schedule_date,
  slot_start,
  slot_end,
  capacity,
  0,
  0,
  1
FROM slot_gen
WHERE slot_end <= end_time
  AND slot_end > slot_start
ON DUPLICATE KEY UPDATE
  end_time = VALUES(end_time),
  total = VALUES(total),
  status = VALUES(status);
