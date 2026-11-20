-- ============================================================
-- V5：resource 表增加「绑定用户」字段
--
-- 业务背景：
--   资源分两类：STAFF 技师 / ROOM 床位。
--   技师本质上就是商户里的一个「非管理员」员工账号，
--   因此给 resource 增加 user_id，STAFF 资源必须绑定一个用户，
--   ROOM 资源的 user_id 保持为空。
--
-- 为什么用唯一索引：
--   一个用户最多只能绑定一个技师资源，避免同一员工被重复排班。
--   MySQL 的唯一索引允许多个 NULL，所以不影响所有 ROOM 资源。
--
-- 兼容性：
--   user_id 可空，V1 已插入的种子技师资源无需回填。
-- ============================================================

ALTER TABLE resource
  ADD COLUMN user_id BIGINT NULL COMMENT '绑定用户（STAFF 技师必填；ROOM 为空）' AFTER type,
  ADD UNIQUE KEY uk_user (user_id);
