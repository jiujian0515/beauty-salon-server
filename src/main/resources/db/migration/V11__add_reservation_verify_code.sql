-- ============================================================
-- V11：预约单增加「核销码」与核销记录
--
-- 业务要求：预约确认后顾客凭 6 位核销码到店，商户后台核销，
-- 预约单状态 CONFIRMED -> COMPLETED。
-- 历史数据 verify_code 为 NULL（唯一索引允许多个 NULL）。
-- ============================================================

ALTER TABLE reservation
  ADD COLUMN verify_code VARCHAR(8)
    COMMENT '核销码（6位数字，商户内唯一）' AFTER status,
  ADD COLUMN verified_at DATETIME
    COMMENT '核销时间' AFTER cancelled_at,
  ADD COLUMN verified_by BIGINT
    COMMENT '核销人（后台用户 id）' AFTER verified_at,
  ADD UNIQUE KEY uk_merchant_verify_code (merchant_id, verify_code);
