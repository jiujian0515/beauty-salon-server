-- ============================================================
-- V1：初始化表结构 + 种子数据
-- Flyway 会在项目首次启动时自动执行本文件（只需启动一次，之后会被记录为已执行）
--
-- 命名规则：V{版本号}__{描述}.sql，版本号必须递增
-- 以后改表结构时新增 V2__xxx.sql、V3__xxx.sql ...，不要修改已执行过的文件
--
-- 通用约定（见 README-BTD.md 第 6 节）：
--   主键 BIGINT 自增；金额一律 BIGINT 单位「分」；时间 DATETIME；
--   字符集 utf8mb4；引擎 InnoDB。
--
-- 账号模型（两套账号池，物理分表）：
--   user     —— 后台账号：管理员 / 员工，必须绑定商户，登录走 /api/v1/auth/login
--   customer —— 顾客账号：全局共享（不绑定商户），来源有两个
--               1) 顾客端自助注册 /api/v1/customer/auth/register
--               2) 后台管理员添加 /api/v1/admin/customers
--   两者手机号允许重复（同一个人既可能是某店员工，也可能是顾客），
--   因此登录态用 "user:{id}" / "customer:{id}" 前缀区分，互不干扰。
--
-- 权限模型（参考若依 RuoYi）：
--   后台账号与角色是「多对多」关系，通过 user_role 关联表维护，
--   user 表不再直接存 role 字段；顾客不参与后台角色体系。
--   初始化只内置一个「管理员」角色和一个管理员账号，
--   其余角色由管理员通过角色管理接口自行维护。
-- ============================================================

-- ---------- 1. 商户表（演示为单租户，但预留 merchant_id 便于扩展多租户） ----------
CREATE TABLE merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '商户名称',
  phone VARCHAR(20) COMMENT '联系电话',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户';

-- ---------- 2. 角色表（角色字典，初始化仅内置管理员角色） ----------
CREATE TABLE role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '角色名称（中文显示用）',
  code VARCHAR(32) NOT NULL COMMENT '角色代码（如 ADMIN/STAFF，与用户关联后用于鉴权）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ---------- 3. 后台用户表（管理员/员工，必须绑定商户；顾客见 customer 表） ----------
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户（管理员/员工必填）',
  phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
  password_hash VARCHAR(128) COMMENT '密码哈希',
  nickname VARCHAR(64) COMMENT '昵称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户（管理员/员工）';

-- ---------- 3.1 顾客表（全局共享，不绑定商户；支持顾客端自助注册） ----------
CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
  password_hash VARCHAR(128) COMMENT '密码哈希',
  nickname VARCHAR(64) COMMENT '昵称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客';

-- ---------- 4. 用户-角色关联表（多对多，参考若依 sys_user_role） ----------
CREATE TABLE user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户',
  role_id BIGINT NOT NULL COMMENT '角色',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ---------- 5. 服务项目表 ----------
CREATE TABLE service_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  name VARCHAR(128) NOT NULL COMMENT '服务名称',
  description VARCHAR(512) COMMENT '服务描述',
  duration_minutes INT NOT NULL COMMENT '服务时长（分钟）',
  price_cents BIGINT NOT NULL COMMENT '价格（分）',
  deposit_cents BIGINT NOT NULL COMMENT '定金（分）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务项目';

-- ---------- 6. 资源表（技师 STAFF / 床位 ROOM） ----------
CREATE TABLE resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  type VARCHAR(16) NOT NULL COMMENT '类型：STAFF 技师 / ROOM 床位',
  name VARCHAR(64) NOT NULL COMMENT '资源名称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源';

-- ---------- 7. 排班表（商户为资源设置的可售时间窗口） ----------
CREATE TABLE schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  resource_id BIGINT NOT NULL COMMENT '资源',
  schedule_date DATE NOT NULL COMMENT '排班日期',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  capacity INT NOT NULL DEFAULT 1 COMMENT '容量（同一时段可同时服务的数量）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1有效 0无效',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_res_date_start (resource_id, schedule_date, start_time),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班';

-- ---------- 8. 时段库存表（独立成行，用于并发防超卖） ----------
CREATE TABLE slot_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  resource_id BIGINT NOT NULL COMMENT '资源',
  slot_date DATE NOT NULL COMMENT '时段日期',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  total INT NOT NULL DEFAULT 1 COMMENT '总容量',
  used INT NOT NULL DEFAULT 0 COMMENT '已占用数',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_res_date_start (resource_id, slot_date, start_time),
  KEY idx_merchant (merchant_id),
  KEY idx_date (slot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时段库存';

-- ---------- 9. 预约单表 ----------
CREATE TABLE reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  reservation_no VARCHAR(32) NOT NULL COMMENT '预约业务号',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  customer_id BIGINT NOT NULL COMMENT '顾客',
  service_item_id BIGINT NOT NULL COMMENT '服务项目',
  resource_id BIGINT NOT NULL COMMENT '资源',
  slot_inventory_id BIGINT NOT NULL COMMENT '时段库存',
  appointment_time DATETIME NOT NULL COMMENT '预约时间',
  deposit_cents BIGINT NOT NULL COMMENT '定金（分）',
  status VARCHAR(24) NOT NULL COMMENT '状态：PENDING_PAYMENT/CONFIRMED/COMPLETED/CANCELLED/EXPIRED/REFUNDING/REFUNDED',
  paid_at DATETIME COMMENT '支付时间',
  cancelled_at DATETIME COMMENT '取消时间',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reservation_no (reservation_no),
  KEY idx_status_created (status, created_at),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约单';

-- ---------- 10. 定金流水表 ----------
CREATE TABLE deposit_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  reservation_id BIGINT NOT NULL COMMENT '预约单',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号',
  out_trade_no VARCHAR(64) NOT NULL COMMENT '平台交易号（幂等键）',
  channel VARCHAR(16) NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
  amount_cents BIGINT NOT NULL COMMENT '金额（分）',
  status VARCHAR(16) NOT NULL COMMENT '状态：PENDING/SUCCESS/FAILED/EXPIRED/REFUNDED',
  paid_at DATETIME COMMENT '支付时间',
  refunded_at DATETIME COMMENT '退款时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_out_trade_no (out_trade_no),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_reservation (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定金流水';

-- ---------- 11. 审计日志表（只追加，不更新） ----------
CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  merchant_id BIGINT NOT NULL COMMENT '所属商户',
  actor_id BIGINT COMMENT '操作人',
  action VARCHAR(64) NOT NULL COMMENT '动作（如 CREATE_RESERVATION/VERIFY/REFUND）',
  target_type VARCHAR(32) COMMENT '目标类型',
  target_id BIGINT COMMENT '目标 id',
  detail VARCHAR(1024) COMMENT '详情',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';



-- ============================================================
-- 种子数据（演示用）
-- ============================================================

-- 演示商户
INSERT INTO merchant (id, name, phone) VALUES (1, '颜汐美容院', '13800000000');

-- 角色字典：初始化只内置管理员角色（其余角色由管理员自行创建）
INSERT INTO role (id, name, code) VALUES (1, '管理员', 'ADMIN');

-- 初始用户：只内置一个管理员账号
-- 密码字段为占位值，由 V2 填充真实的 BCrypt 哈希（明文 123456）
INSERT INTO user (id, merchant_id, phone, password_hash, nickname) VALUES
  (1, 1, '13800000001', 'TODO_BCRYPT_ADMIN', '管理员');

-- 管理员账号绑定管理员角色（用户与角色通过关联表维护）
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);

-- 资源：STAFF 技师 / ROOM 床位
INSERT INTO resource (id, merchant_id, type, name, status) VALUES
  (1, 1, 'STAFF', '技师小王', 1),
  (2, 1, 'ROOM',  '床位01',   1);

-- 排班：为两个资源生成未来 7 天、每天 9:00-18:00 的排班
-- 使用 CURDATE() + INTERVAL 保证任何时候都有「未来」排班，方便演示
INSERT INTO schedule (merchant_id, resource_id, schedule_date, start_time, end_time, capacity, status)
SELECT 1, r.id, CURDATE() + INTERVAL d.day DAY, '09:00:00', '18:00:00', 1, 1
FROM resource r
CROSS JOIN (
  SELECT 0 AS day UNION SELECT 1 UNION SELECT 2 UNION SELECT 3
  UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
) d
WHERE r.merchant_id = 1;

-- 时段库存：这里仅示范 1 条。
-- 正常应由「排班生成库存」的服务代码按服务时长拆分生成（见 PTD 5.6）。
INSERT INTO slot_inventory (merchant_id, resource_id, slot_date, start_time, end_time, total, used, version)
VALUES (1, 1, CURDATE() + INTERVAL 1 DAY, '09:00:00', '10:00:00', 1, 0, 0);
