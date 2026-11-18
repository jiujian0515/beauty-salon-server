-- ============================================================
-- V2：填充种子用户的真实 BCrypt 密码哈希
--
-- 为什么是 V2 而不是改 V1？
--   V1 已经在你的数据库里执行过了（flyway_schema_history 有记录）。
--   如果修改 V1，Flyway 会因校验和(checksum)不一致而报错。
--   所以每次改数据库都要「新增」一个版本号更大的文件。
--
-- 本文件做三件事：
--   1. 把 V1 里的占位密码（TODO_BCRYPT_*）替换成真正的 BCrypt 哈希
--   2. 三个账号的统一演示密码都是：123456
--   3. 哈希是用项目里的 BCryptPasswordEncoder 生成的（和你登录时校验用的同一套算法）
--
-- 说明：BCrypt 每次加密都会自动加随机「盐」，所以三个哈希值不同，
--       但它们对应的明文都是 123456。
-- ============================================================

-- 管理员（手机号 13800000001 / 密码 123456）
UPDATE user SET password_hash = '$2a$10$0fgkb4J.CsK2q/zqumRTN.JAoi/AEpvLhGwoo3ZwHh9XDcGvJ/zn6'
WHERE phone = '13800000001';

-- 员工（手机号 13800000002 / 密码 123456）
UPDATE user SET password_hash = '$2a$10$grQ/5MhzLj5oH7ifhvhwpuK5pVnsEOcdaKeBQJNUKRov.r/Yw9cnW'
WHERE phone = '13800000002';

-- 顾客（手机号 13800000003 / 密码 123456）
UPDATE user SET password_hash = '$2a$10$wcSBJeoAKBGEwD4cHnBOWug9hSsHqJPaOXOLqlGfV6pilILFYTJzW'
WHERE phone = '13800000003';
