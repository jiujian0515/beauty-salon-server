-- ============================================================
-- V7：顾客表增加微信小程序 openid
--
-- 用途：
--   微信手机号快速登录时，除了手机号，还保存微信 openid，
--   便于后续识别「同一个微信用户」。
--
-- 为什么用 UNIQUE KEY：
--   一个 openid 只应绑定一个顾客账号，避免同一微信身份对应多个账号。
--   MySQL 唯一索引允许多个 NULL，所以历史顾客数据不受影响。
-- ============================================================

ALTER TABLE customer
  ADD COLUMN wechat_openid VARCHAR(64) NULL COMMENT '微信小程序 openid' AFTER password_hash;

ALTER TABLE customer
  ADD UNIQUE KEY uk_wechat_openid (wechat_openid);
