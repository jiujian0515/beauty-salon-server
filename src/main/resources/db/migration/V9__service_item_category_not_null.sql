-- ============================================================
-- V9：服务分类改为必填
--
-- 业务要求：服务项目必须有分类，不允许为空。
-- 历史数据用「未分类」补齐后，将列改为 NOT NULL 并保留默认值兜底。
-- ============================================================

UPDATE service_item SET category = '未分类' WHERE category IS NULL OR category = '';

ALTER TABLE service_item
  MODIFY COLUMN category VARCHAR(64) NOT NULL DEFAULT '未分类' COMMENT '服务分类（商户自定义）';
