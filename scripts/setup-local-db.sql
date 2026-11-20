-- ============================================================
-- 本地开发一次性初始化脚本（只需执行一次）
-- 作用：创建 beauty_salon 数据库 + 专用账号 beautysalon，
--       供 src/main/resources/application-dev.yml 连接使用。
--
-- 表结构不用手动建：项目启动时 Flyway 会自动执行
-- src/main/resources/db/migration/V1__init.sql 完成建表。
--
-- 执行方式（二选一）：
--   1. PowerShell 命令行（会提示输入 root 密码）：
--        Get-Content scripts\setup-local-db.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p
--   2. 或用 Navicat / Workbench 等图形工具打开本文件直接执行。
-- ============================================================

-- 1. 创建数据库（字符集 utf8mb4，兼容中文和 emoji）
CREATE DATABASE IF NOT EXISTS beauty_salon
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 2. 创建专用账号（避免直接用 root）
--    密码 beautysalon123 需与 application-dev.yml 中的 password 保持一致
CREATE USER IF NOT EXISTS 'beautysalon'@'localhost' IDENTIFIED BY 'beautysalon123';

-- 3. 授予该账号对 beauty_salon 库的全部权限
GRANT ALL PRIVILEGES ON beauty_salon.* TO 'beautysalon'@'localhost';

-- 4. 刷新权限使其立即生效
FLUSH PRIVILEGES;
