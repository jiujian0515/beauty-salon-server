# 美容院轻量 SaaS 系统 · 后台服务技术文档（BTD）

> 文档状态：评审中
> 最后更新：2026-11-18
> 关联文档：README-PRD.md（需求文档）、README-PTD.md（项目技术文档）
> 适用读者：后端开发、接手开发者、AI 编码助手

## 1. 范围与定位

本文档描述后台服务（Spring Boot + MyBatis-Plus + MySQL）的工程结构、分层职责、数据库 DDL、核心模块实现、配置与测试。与 PTD 的关系：PTD 给「技术方案」，本文档给「落地实现」。

## 2. 工程结构

```
beauty-salon-server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── src/main/
    ├── java/com/beautysalon/
    │   ├── BeautySalonApplication.java
    │   ├── common/        # 统一响应 Result、业务异常、错误码 ErrorCode、常量
    │   ├── config/        # Sa-Token、MyBatis-Plus 分页、Web、Jackson、定时任务
    │   ├── controller/    # 接口层（薄，只做参数校验与调用）
    │   ├── service/       # 业务接口 + impl（核心逻辑与事务）
    │   ├── mapper/        # MyBatis-Plus Mapper（复杂 SQL 用 XML）
    │   ├── model/
    │   │   ├── entity/    # 与表对应的实体
    │   │   ├── dto/       # 请求/响应对象
    │   │   └── enums/     # 状态枚举（ReservationStatus 等）
    │   ├── job/           # 定时任务（超时释放）
    │   └── integration/   # 支付网关（演示为模拟实现）
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── db/migration/  # Flyway：V1__init.sql ...
```

## 3. 分层与职责

| 层 | 职责 | 约束 |
|---|---|---|
| controller | 参数校验、鉴权注解、调用 service、组装 Result | 不含业务逻辑 |
| service | 业务规则、状态机、事务边界 | 事务注解在此层 |
| mapper | 数据访问、原子条件更新 SQL | 复杂 SQL 写 XML |
| model/entity | 表映射 | 用 MyBatis-Plus 注解 |
| common | 统一响应、错误码、异常 | 全局异常处理返回 Result |
| job | 定时扫描与释放 | 幂等、可重入 |

## 4. 统一响应与错误码

统一响应结构：

```json
{ "code": 0, "message": "success", "data": {} }
```

错误码约定：

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40001 | 参数错误 |
| 40101 | 未登录 / 登录失效 |
| 40301 | 无权限 |
| 40401 | 资源不存在 |
| 40901 | 时段已满（库存不足） |
| 40902 | 预约状态不允许该操作 |
| 40903 | 距离服务开始不足 24 小时，不可取消 |
| 40904 | 数据已存在（唯一字段冲突，如手机号重复） |
| 50000 | 系统异常 |

- 全局异常处理器将业务异常映射为对应 code 的 Result。
- 金额字段一律 BIGINT（分），DTO 对外输出以「分」为整数或转换为元字符串，禁止 double。

## 5. 配置（application.yml 关键项）

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:beauty_salon}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted

sa-token:
  token-name: Authorization
  timeout: 2592000

app:
  timezone: Asia/Shanghai
  payment:
    mock: true              # 演示环境开启模拟支付
    expire-minutes: 120     # 待支付占位时限
    cancel-ahead-hours: 24  # 可取消提前量
```

- 时区统一 Asia/Shanghai：连接串 serverTimezone + JVM -Duser.timezone=Asia/Shanghai + @Scheduled zone。

## 6. 数据库表 DDL

> 全部 BIGINT 主键自增，金额 BIGINT 分，时间 DATETIME，字符集 utf8mb4，引擎 InnoDB。

### 6.1 merchant

```sql
CREATE TABLE merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(20),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.2 user / role / user_role / customer（两套账号池）

> 账号分成两套彼此独立的「账号池」，靠登录标识前缀隔离：
>
> | 账号池 | 表 | 登录接口 | Sa-Token 登录标识 | 角色来源 |
> |---|---|---|---|---|
> | 后台账号（管理员/员工） | `user` | `POST /auth/login` | `user:{id}` | `user_role` 关联表 |
> | 顾客 | `customer` | `POST /customer/auth/login` | `customer:{id}` | 无（顾客永远没有后台角色） |
>
> 为什么必须拆表：顾客可以自助注册、数量级远大于员工、且不绑定商户（全局共享），
> 和「必须属于某个商户、必须被管理员分配角色」的后台账号是完全不同的生命周期。
> 两表手机号各自唯一（可以重复），所以登录标识必须带前缀，否则 `StpUtil.getLoginId()` 无法区分。
>
> 参考若依 RuoYi 的 sys_user / sys_role / sys_user_role：后台账号与角色是「多对多」，
> `user` 表不再直接存角色字段，角色关系由 `user_role` 关联表维护。
> 初始化只内置一个「管理员」角色（code = ADMIN）和一个管理员账号（13800000001 / 123456），
> 其余角色由管理员通过角色管理接口自行创建。

```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL COMMENT '所属商户（后台账号必须归属某个商户）',
  phone VARCHAR(20) NOT NULL,
  password_hash VARCHAR(128),
  nickname VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0正常',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  code VARCHAR(32) NOT NULL COMMENT '如 ADMIN/STAFF/CUSTOMER，鉴权用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  password_hash VARCHAR(128),
  nickname VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0正常',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

业务约束：

- `ADMIN` 角色是内置角色：不允许创建、修改、删除；`ADMIN` 账号的角色不允许被其他用户修改，也不允许删除/禁用。
- 角色仍被用户引用时不允许删除（返回 40905）。
- `CUSTOMER` 是顾客保留代码：角色管理里禁止创建或改成 CUSTOMER，后台账号也不能挂 CUSTOMER 角色。
- 后台账号必须绑定商户（`merchant_id NOT NULL`），只能看到/管理本商户的账号。
- 顾客全局共享、不绑定商户：后台的顾客列表不做商户过滤；顾客账号由后台添加或顾客端自助注册两条路径产生。
- 删除账号时先把手机号改写为 `phone + '#del' + id` 再逻辑删除，释放手机号唯一索引以便复用。

### 6.3 service_item

```sql
CREATE TABLE service_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  duration_minutes INT NOT NULL,
  price_cents BIGINT NOT NULL,
  deposit_cents BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.4 resource

```sql
CREATE TABLE resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL COMMENT 'STAFF/ROOM',
  name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.5 schedule

```sql
CREATE TABLE schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  capacity INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_res_date_start (resource_id, schedule_date, start_time),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.6 slot_inventory

```sql
CREATE TABLE slot_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  total INT NOT NULL DEFAULT 1,
  used INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_res_date_start (resource_id, slot_date, start_time),
  KEY idx_merchant (merchant_id),
  KEY idx_date (slot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.7 reservation

```sql
CREATE TABLE reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  service_item_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  slot_inventory_id BIGINT NOT NULL,
  appointment_time DATETIME NOT NULL,
  deposit_cents BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL COMMENT 'PENDING_PAYMENT/CONFIRMED/COMPLETED/CANCELLED/EXPIRED/REFUNDING/REFUNDED',
  paid_at DATETIME,
  cancelled_at DATETIME,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reservation_no (reservation_no),
  KEY idx_status_created (status, created_at),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.8 deposit_transaction

```sql
CREATE TABLE deposit_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  reservation_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  out_trade_no VARCHAR(64) NOT NULL COMMENT '平台交易号，幂等键',
  channel VARCHAR(16) NOT NULL DEFAULT 'MOCK',
  amount_cents BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'PENDING/SUCCESS/FAILED/EXPIRED/REFUNDED',
  paid_at DATETIME,
  refunded_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_out_trade_no (out_trade_no),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_reservation (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.9 audit_log

```sql
CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  actor_id BIGINT,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32),
  target_id BIGINT,
  detail VARCHAR(1024),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 7. API 契约

统一前缀 /api/v1，返回 Result 结构。

### 7.1 顾客端

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /customer/auth/register | 顾客自助注册（手机号+密码），注册即登录返回 token | 否 |
| POST | /customer/auth/login | 顾客登录 | 否 |
| POST | /customer/auth/logout | 顾客退出 | 顾客 |
| GET | /customer/auth/me | 当前顾客信息 | 顾客 |
| GET | /services | 服务项目列表 | 否 |
| GET | /slots | 可售时段（date、serviceId 参数） | 否 |
| POST | /reservations | 创建预约（返回订单号+支付参数） | 顾客 |
| POST | /payments/mock-pay | 模拟支付（演示触发成功回调） | 顾客 |
| POST | /payments/callback | 支付平台回调 | 签名 |
| GET | /reservations/mine | 我的预约 | 顾客 |
| POST | /reservations/{id}/cancel | 取消预约 | 顾客 |

### 7.2 商户端

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| POST | /auth/login | 后台账号（管理员/员工）登录 | 否 |
| GET | /auth/me | 当前后台账号信息（含角色） | 后台账号 |
| GET/POST/PUT/DELETE | /admin/users | 后台账号 CRUD（仅管理员/员工，不含顾客） | ADMIN |
| PUT | /admin/users/{id}/password | 重置后台账号密码 | ADMIN |
| GET/POST/PUT/DELETE | /admin/roles | 角色 CRUD（ADMIN 角色不可改/删/重复创建） | ADMIN |
| GET/POST/PUT/DELETE | /admin/customers | 顾客 CRUD（全局共享，支持 keyword 模糊查询） | ADMIN |
| PUT | /admin/customers/{id}/password | 重置顾客密码 | ADMIN |
| GET/POST/PUT/DELETE | /admin/services | 服务项目 CRUD | ADMIN |
| GET/POST/PUT/DELETE | /admin/resources | 资源 CRUD | ADMIN |
| GET/POST/PUT/DELETE | /admin/schedules | 排班 CRUD | STAFF/ADMIN |
| GET | /admin/calendar | 日历视图 | STAFF/ADMIN |
| POST | /admin/slots/{id}/lock | 锁定时段 | STAFF/ADMIN |
| POST | /admin/slots/{id}/unlock | 释放时段 | STAFF/ADMIN |
| GET | /admin/reservations | 预约列表 | STAFF/ADMIN |
| POST | /admin/reservations/{id}/verify | 发起核销 | STAFF/ADMIN |

## 8. 核心模块实现要点

### 8.1 创建预约（下单）

伪代码（ReservationService.create）：

```text
1. 校验服务项目存在且上架
2. 校验预约时间 >= now + 24h（app.cancel-ahead-hours 相关规则）
3. 计算对应 slot_inventory 行（resource + date + start）
4. 开事务：
   a. UPDATE slot_inventory SET used=used+1 WHERE id=? AND used<total
      影响行数=0 → 抛 40901「时段已满」
   b. INSERT reservation(status=PENDING_PAYMENT)
   c. INSERT deposit_transaction(status=PENDING)
5. 返回 order_no + 支付参数（演示环境为模拟支付链接）
```

### 8.2 支付回调

伪代码（PaymentService.handleCallback）：

```text
1. 校验回调签名（演示环境跳过）
2. UPDATE deposit_transaction SET status='SUCCESS' WHERE out_trade_no=? AND status='PENDING'
   影响行数=0 → 直接返回 success（幂等）
3. UPDATE reservation SET status='CONFIRMED', paid_at=NOW()
   WHERE id=? AND status='PENDING_PAYMENT'
4. 写 audit_log
5. 返回 success
```

### 8.3 超时释放

伪代码（Job 每分钟）：

```text
1. 查询 status='PENDING_PAYMENT' AND created_at < NOW()-2h 的 reservation（分页）
2. 逐单：
   UPDATE reservation SET status='EXPIRED' WHERE id=? AND status='PENDING_PAYMENT'
   影响行数=1 时：UPDATE slot_inventory SET used=used-1 WHERE id=? AND used>0
                 UPDATE deposit_transaction SET status='EXPIRED' WHERE reservation_id=? AND status='PENDING'
```

### 8.4 取消与退款

```text
1. 校验预约状态 = CONFIRMED
2. 校验 now <= appointment_time - 24h（否则抛 40903）
3. 事务：
   a. reservation: CONFIRMED → REFUNDING
   b. deposit_transaction: SUCCESS → REFUNDED（触发退款，演示为模拟）
   c. slot_inventory: used-1 释放
   d. reservation: REFUNDING → REFUNDED（或 CANCELLED）
4. 写 audit_log
```

## 9. 鉴权与数据隔离

- 登录：Sa-Token，token 放 Authorization 头；顾客/员工分登录接口。
- 注解：@SaCheckLogin、@SaCheckRole("ADMIN")。
- 数据隔离：service 层所有查询强制带 merchant_id（从登录态或租户上下文取），防止越权访问他商户数据。

## 10. 定时任务

| 任务 | 频率 | 说明 |
|---|---|---|
| 超时释放 | 每分钟 | 见 8.3 |
| 到期提醒（可选） | 每小时 | 预约前 N 小时提醒（演示可略） |

- 均显式 zone = Asia/Shanghai，逻辑幂等可重入。

## 11. 测试

- 单元测试：状态机流转、取消规则（24h 边界）。
- 集成测试：
  - 并发抢库存：10 线程并发下单 1 库存，仅 1 成功。
  - 回调幂等：同 out_trade_no 回调 3 次，仅 1 次生效。
  - 超时释放：插入过期待支付单，跑 job，断言库存释放。
- 契约测试：Apifox 对第 7 节接口做断言。

## 12. 本地运行与部署

- 本地：docker compose up -d mysql，配置 dev 环境，mvn spring-boot:run。
- 生产：docker compose up -d（Nginx + app + MySQL），环境变量注入密钥。
