-- ============================================================
-- V8：服务项目新增分类字段
--
-- 分类为商户自定义的文本标签（如「面部护理」「美甲」），
-- 列表接口支持按分类精确筛选，故补充 (merchant_id, category) 联合索引。
-- 历史数据 category 为空，前端可显示为「未分类」。
-- ============================================================

ALTER TABLE service_item
  ADD COLUMN category VARCHAR(64) NULL COMMENT '服务分类（商户自定义）' AFTER name,
  ADD KEY idx_merchant_category (merchant_id, category);
