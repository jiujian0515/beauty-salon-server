-- ============================================================
-- V3：user 表增加「逻辑删除」字段
--
-- 为什么员工删除要「逻辑删除」而不是物理 DELETE？
--   员工可能已经产生过排班、预约记录，直接 DELETE 会让历史数据失去关联。
--   逻辑删除只是把 deleted 置为 1，数据仍在库里，历史可追溯。
--
-- MyBatis-Plus 的行为（application.yml 里已配置 logic-delete-field: deleted）：
--   deleteById(id)  → 自动变成 UPDATE user SET deleted=1 WHERE id=? AND deleted=0
--   所有查询         → 自动追加 WHERE deleted=0（被删数据查不出来）
-- ============================================================

ALTER TABLE user
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0正常' AFTER status;

-- 员工列表查询常用「商户 + 角色」组合过滤，加联合索引
ALTER TABLE user
  ADD INDEX idx_merchant_role (merchant_id, role);
