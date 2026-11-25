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
  wechat:
    app-id: ${WECHAT_APP_ID:}         # 微信小程序 AppID，通过环境变量注入
    app-secret: ${WECHAT_APP_SECRET:} # 微信小程序 AppSecret，绝不写死在仓库
    api-base-url: https://api.weixin.qq.com
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
> | 顾客 | `customer` | `POST /customer/auth/login`、`POST /customer/auth/wechat/login` | `customer:{id}` | 无（顾客永远没有后台角色） |
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
  wechat_openid VARCHAR(64) COMMENT '微信小程序 openid',
  nickname VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0正常',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_wechat_openid (wechat_openid)
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
  user_id BIGINT NULL COMMENT '绑定用户（STAFF 技师必填；ROOM 为空）',
  name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id),
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
  slot_minutes INT NOT NULL DEFAULT 60 COMMENT '库存拆分步长（分钟）',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1可售 0锁定',
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
  is_course TINYINT NOT NULL DEFAULT 0 COMMENT '是否疗程预约：1是（免定金） 0否',
  status VARCHAR(24) NOT NULL COMMENT 'PENDING_PAYMENT/CONFIRMED/COMPLETED/CANCELLED/EXPIRED/REFUNDING/REFUNDED',
  verify_code VARCHAR(8) COMMENT '核销码（6位数字，商户内唯一）',
  paid_at DATETIME,
  cancelled_at DATETIME,
  verified_at DATETIME COMMENT '核销时间',
  verified_by BIGINT COMMENT '核销人（后台用户 id）',
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reservation_no (reservation_no),
  UNIQUE KEY uk_merchant_verify_code (merchant_id, verify_code),
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
| POST | /customer/auth/login | 顾客手机号 + 密码登录 | 否 |
| POST | /customer/auth/wechat/login | 微信手机号快速登录（loginCode + phoneCode，未注册自动创建账号） | 否 |
| POST | /customer/auth/logout | 顾客退出 | 顾客 |
| GET | /customer/auth/me | 当前顾客信息 | 顾客 |
| GET | /customer/profile | 当前顾客个人信息 + 预约统计（累计成功/待服务/待支付） | 顾客 |
| GET | /services | 服务项目列表（分页：page、size、keyword、merchantId、category，仅返回上架） | 否 |
| GET | /resources | 美容师列表（分页：page、size、keyword、merchantId、type，仅返回启用资源，type 默认 STAFF） | 否 |
| GET | /slots | 可售时段（date 必填；resourceId、merchantId 可选，仅返回可预约时段） | 否 |
| POST | /reservations | 创建预约（普通预约返回订单号+支付参数；疗程预约免定金直接确认并返回核销码） | 顾客 |
| POST | /payments/mock-pay | 模拟支付（入参 outTradeNo，演示触发成功回调，幂等） | 顾客 |
| POST | /payments/callback | 支付平台异步回调（HMAC-SHA256 验签，返回纯文本 SUCCESS/FAIL） | 签名 |
| GET | /reservations/mine | 我的预约（状态为 CONFIRMED/COMPLETED 时返回核销码 verifyCode） | 顾客 |
| POST | /reservations/{id}/cancel | 取消预约 | 顾客 |

- 核销码 `verifyCode` 在创建预约时生成（6 位数字，商户内唯一），仅当状态为 `CONFIRMED`/`COMPLETED` 时返回给顾客；普通预约需支付成功后才可见。

`GET /customer/profile` 响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "customerId": 1,
    "phone": "13800002108",
    "nickname": "林舒意",
    "status": 1,
    "reservationStats": {
      "completedCount": 3,
      "pendingServiceCount": 1,
      "pendingPaymentCount": 0
    }
  }
}
```

- `completedCount`：累计成功预约，统计 `COMPLETED`（已完成）。
- `pendingServiceCount`：待服务，统计 `CONFIRMED`（已确认、待到店）。
- `pendingPaymentCount`：待支付，统计 `PENDING_PAYMENT`。
- 统计只针对当前登录顾客；无预约时三项均为 `0`。

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
| GET/POST/PUT/DELETE | /admin/services | 服务项目 CRUD（列表支持 category 分类筛选） | ADMIN |
| GET/POST/PUT/DELETE | /admin/resources | 资源 CRUD（STAFF 技师需绑定非管理员用户） | ADMIN |
| GET | /admin/resources/staff-candidates | 技师候选用户（本商户非管理员、未绑定） | ADMIN |
| GET/POST/PUT/DELETE | /admin/schedules | 排班 CRUD（保存时按 slotMinutes 自动生成时段库存） | 后台账号 |
| POST | /admin/schedules/batch | 批量排班（按星期过滤，冲突日期跳过并返回原因） | 后台账号 |
| POST | /admin/schedules/batch-delete | 批量删除排班（有预约占用的跳过并返回原因） | 后台账号 |
| GET | /admin/schedules/{id}/slots | 排班窗口内的时段库存 | 后台账号 |
| GET | /admin/calendar | 日历视图（默认今天起 7 天，最多 62 天） | 后台账号 |
| GET | /admin/slots | 时段库存分页（resourceId/日期/status 过滤） | 后台账号 |
| POST | /admin/slots/{id}/lock | 锁定时段 | 后台账号 |
| POST | /admin/slots/{id}/unlock | 释放时段 | 后台账号 |
| GET | /admin/reservations | 核销单列表（status、verifyCode、keyword、startDate、endDate、page、size） | STAFF/ADMIN |
| GET | /admin/reservations/{id} | 核销单详情（仅限本商户） | STAFF/ADMIN |
| POST | /admin/reservations/verify | 凭核销码核销：CONFIRMED → COMPLETED | STAFF/ADMIN |

- `keyword` 同时匹配预约单号、核销码、顾客手机号/昵称；`startDate`/`endDate` 按预约时间过滤。
- 核销接口入参为 `{"verifyCode":"123456"}`，不依赖预约 id。

## 8. 核心模块实现要点

### 8.1 创建预约（下单）

请求 `POST /api/v1/reservations`（顾客登录）：

```json
{
  "serviceItemId": 1,
  "slotId": 1,
  "isCourse": false
}
```

- `isCourse=false`：普通预约，按服务项目定金生成待支付预约单与定金流水。
- `isCourse=true`：疗程预约，定金强制为 0，不生成定金流水，创建后直接 `CONFIRMED`。

普通预约响应示例：

```json
{
  "reservationId": 1,
  "reservationNo": "R20261124153000123456",
  "status": "PENDING_PAYMENT",
  "verifyCode": null,
  "isCourse": false,
  "depositCents": 5000,
  "paymentRequired": true,
  "orderNo": "P20261124153000654321",
  "appointmentTime": "2026-11-26 09:00:00",
  "merchantId": 1,
  "resourceId": 1,
  "resourceName": "技师小王",
  "slotId": 1,
  "paymentParams": {
    "channel": "MOCK",
    "outTradeNo": "T20261124153000987654",
    "amountCents": 5000,
    "mockPay": true
  }
}
```

疗程预约响应：`status=CONFIRMED`、`isCourse=true`、`depositCents=0`、`paymentRequired=false`、
`orderNo` 与 `paymentParams` 均为 `null`，并返回 6 位核销码 `verifyCode`。
普通预约在支付成功转为 `CONFIRMED` 后，`GET /reservations/mine` 才会返回该核销码。

伪代码（ReservationService.create）：

```text
1. 校验服务项目存在且上架
2. 校验时段存在、可售、有剩余容量，且与服务项目属于同一商户、资源启用
3. 校验预约时间 >= now + lead-hours（默认 24h）
4. 计算定金：isCourse ? 0 : service_item.deposit_cents
5. 开事务：
   a. UPDATE slot_inventory SET used=used+1, version=version+1
      WHERE id=? AND used<total AND status=1
      影响行数=0 → 抛 40901「时段已满」
   b. INSERT reservation(
        status = 需支付 ? PENDING_PAYMENT : CONFIRMED,
        is_course = isCourse ? 1 : 0,
        deposit_cents = 定金,
        verify_code = 商户内唯一的 6 位随机数字
      )
   c. 需支付时 INSERT deposit_transaction(status=PENDING)
6. 需支付：返回 order_no + 模拟支付参数；否则直接返回已确认预约
```

### 8.2 模拟支付

请求 `POST /api/v1/payments/mock-pay`（顾客登录）：

```json
{
  "outTradeNo": "T20261124153000987654"
}
```

响应示例：

```json
{
  "outTradeNo": "T20261124153000987654",
  "orderNo": "P20261124153000654321",
  "reservationId": 1,
  "reservationNo": "R20261124153000123456",
  "status": "CONFIRMED",
  "amountCents": 5000,
  "paidAt": "2026-11-24 15:31:02"
}
```

演示环境直接复用支付回调成功逻辑，以 `out_trade_no` 为幂等键：

```text
1. 校验当前顾客；按 out_trade_no 查流水，不存在 → 40401
2. 校验预约归属当前顾客，否则 → 40301
3. 流水已 SUCCESS → 幂等返回（必要时补确认预约）
4. 流水非 PENDING 或预约非 PENDING_PAYMENT → 40902
5. UPDATE deposit_transaction SET status='SUCCESS', paid_at=NOW()
   WHERE out_trade_no=? AND status='PENDING'
   影响行数=0 → 重查确认已 SUCCESS，否则 40902
6. UPDATE reservation SET status='CONFIRMED', paid_at=NOW()
   WHERE id=? AND status='PENDING_PAYMENT'
   影响行数=0 → 重查确认已 CONFIRMED，否则 40902（事务回滚）
7. 预约本次确认成功时写 audit_log（action=PAY_RESERVATION）
8. 返回预约最新状态、订单号、金额、支付时间
```

- 库存在创建预约时已占用，支付成功不重复扣减。
- 模拟支付只允许操作当前顾客自己的订单；真实支付平台的异步通知走 8.3 的回调接口。

### 8.3 支付平台回调

请求 `POST /api/v1/payments/callback`（匿名，无登录态，靠签名鉴权）：

```json
{
  "outTradeNo": "T20261124153000987654",
  "tradeStatus": "SUCCESS",
  "amountCents": 5000
}
```

请求头 `X-Pay-Signature: <hex>`（也兼容报文体 `sign` 字段）。

签名算法（HMAC-SHA256，密钥为 `app.payment.callback-secret`）：

```text
1. 取报文体全部字段，排除 sign / signature
2. 按 key 字典序升序拼成 k1=v1&k2=v2
3. HMAC-SHA256(secret, 待签名串)，输出小写十六进制
```

示例待签名串：`amountCents=5000&outTradeNo=T20261124153000987654&tradeStatus=SUCCESS`

响应为**纯文本**（不套 Result 结构，符合支付平台惯例）：

- `SUCCESS`：已受理（含重复回调），平台无需重试
- `FAIL`：验签失败 / 金额不符 / 状态非法 / 系统异常，平台按策略重试

处理流程：

```text
1. 解析 JSON 报文；验签失败 → FAIL
2. 校验 outTradeNo、tradeStatus、amountCents 必填
3. 按 out_trade_no 查流水，不存在 → FAIL
4. 回调金额与流水金额不一致 → FAIL（防篡改/串单）
5. tradeStatus != SUCCESS：记录日志并应答 SUCCESS，不改变业务状态
6. 与模拟支付共用结算逻辑：
   流水 PENDING → SUCCESS（原子），预约 PENDING_PAYMENT → CONFIRMED（原子）
   已 SUCCESS 则幂等返回；本次确认成功才写 audit_log
7. 应答 SUCCESS
```

- 回调入口在 Sa-Token 白名单中，匿名可访问；验签在任何数据库操作之前执行。
- 生产必须通过环境变量 `PAYMENT_CALLBACK_SECRET` 注入密钥；留空时所有回调验签失败。

### 8.4 超时释放

伪代码（Job 每分钟）：

```text
1. 查询 status='PENDING_PAYMENT' AND created_at < NOW()-2h 的 reservation（分页）
2. 逐单：
   UPDATE reservation SET status='EXPIRED' WHERE id=? AND status='PENDING_PAYMENT'
   影响行数=1 时：UPDATE slot_inventory SET used=used-1 WHERE id=? AND used>0
                 UPDATE deposit_transaction SET status='EXPIRED' WHERE reservation_id=? AND status='PENDING'
```

### 8.5 取消与退款

接口：`POST /api/v1/reservations/{id}/cancel`（顾客登录，仅能取消自己的预约）

```text
按预约状态分流：
A. PENDING_PAYMENT（未支付）
   1. 不限时间，直接取消
   2. 事务：reservation: PENDING_PAYMENT → CANCELLED（写 cancelled_at）
           deposit_transaction: PENDING → EXPIRED
           slot_inventory: used-1 释放
B. CONFIRMED（已确认）
   1. 校验 now <= appointment_time - cancel-ahead-hours（默认 24h，否则抛 40903）
   2. 有 SUCCESS 定金流水（模拟退款）：
      a. reservation: CONFIRMED → REFUNDING
      b. deposit_transaction: SUCCESS → REFUNDED（写 refunded_at）
      c. slot_inventory: used-1 释放
      d. reservation: REFUNDING → REFUNDED
   3. 无已支付定金（免定金/疗程）：
      reservation: CONFIRMED → CANCELLED + 释放时段
C. 其它状态 → 40902
所有状态流转均为原子条件更新；重复取消返回当前终态（幂等）
最后写 audit_log（action=CANCEL_RESERVATION, target_type=RESERVATION）
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reservationId": 12,
    "reservationNo": "R202501151030001",
    "status": "REFUNDED",
    "refunded": true,
    "refundAmountCents": 5000,
    "cancelledAt": "2025-01-15 10:30:00"
  }
}
```

- `status`：`CANCELLED`（未退款）或 `REFUNDED`（已退款）
- `refunded`：是否发生退款；`refundAmountCents`：退款金额（分），未退款为 0
- 错误码：`40902` 状态不允许取消、`40903` 不足提前量、`40401` 预约不存在、`40301` 非本人预约

### 8.6 排班与时段库存

```text
保存排班（新增/批量）：
1. 校验资源属于当前商户且 status=1；日期 >= 今天；start < end
2. 校验 slotMinutes ∈ [5,480] 且窗口时长是 slotMinutes 的整数倍；capacity >= 1
3. 同资源同日期窗口不可重叠（newStart < existingEnd && newEnd > existingStart）
4. 按 [start, end) 以 slotMinutes 为步长逐条 upsert slot_inventory
   - 唯一键 (resource_id, slot_date, start_time)，重复执行只更新 end_time/total/status
   - 保留 used，不会重置已占用数
   - 排班 status=0 时生成的时段直接锁定（status=0）

编辑排班：
- 已有 used>0 或存在预约历史 → 禁止改日期/时间/步长（40905），只允许改容量和状态
- 容量不能小于窗口内已占用数
- 无预约历史时改时间：先删旧窗口库存，再按新窗口重建
- status 1↔0：窗口内所有时段一起释放/锁定（会覆盖手工锁定，属预期行为）

删除排班：窗口内任一 used>0 或存在预约历史 → 40905；否则先删库存再删排班
批量删除：逐条处理，不存在/有占用或预约历史的跳过并返回原因，其余正常删除（一次最多 100 条）

锁定/释放时段：
- 锁定 status=0 幂等，不影响 used
- 释放前校验所属排班存在且 status=1，否则报参数错误

日历：按天汇总 scheduleCount/slotCount/totalCapacity/usedCount/availableCount/lockedCount，
无排班的日期也会返回；默认今天起 7 天，最大 62 天
```

- 排班/时段/日历接口当前只做登录校验，角色权限由前端路由控制（后续可补 @SaCheckRole）。

### 8.7 我的预约列表

请求 `GET /api/v1/reservations/mine?page=1&size=10&status=CONFIRMED`（顾客登录）：

| 参数 | 必填 | 说明 |
|---|---|---|
| page | 否 | 页码，默认 1，必须 ≥ 1 |
| size | 否 | 每页条数，默认 10，范围 1~100 |
| status | 否 | 预约状态精确筛选，不区分大小写；非法值返回 40001 |

允许的 `status`：`PENDING_PAYMENT` / `CONFIRMED` / `COMPLETED` / `CANCELLED` / `EXPIRED` / `REFUNDING` / `REFUNDED`。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "reservationId": 1,
        "reservationNo": "R20261124153000123456",
        "status": "CONFIRMED",
        "isCourse": false,
        "depositCents": 5000,
        "appointmentTime": "2026-11-26 09:00:00",
        "merchantId": 1,
        "serviceItemId": 1,
        "serviceItemName": "小气泡清洁",
        "serviceItemDurationMinutes": 60,
        "resourceId": 1,
        "resourceName": "技师小王",
        "resourceType": "STAFF",
        "paidAt": "2026-11-24 15:31:02",
        "cancelledAt": null,
        "createdAt": "2026-11-24 15:30:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10,
    "pages": 1
  }
}
```

- 查询条件强制 `customer_id = 当前登录顾客`，不能查询他人预约。
- 按 `appointment_time DESC, id DESC` 排序。
- 服务项目/资源名称通过批量查询补充，避免列表 N+1；关联数据被删除时对应字段返回 `null`。
- 本次不返回商户名称，仅返回 `merchantId`。
- 状态为 `CONFIRMED`/`COMPLETED` 时返回 `verifyCode`（核销码），其余状态返回 `null`。

### 8.8 到店核销（核销码）

接口（商户登录，`@SaCheckRole(value={"ADMIN","STAFF"}, mode=OR)`）：

- `GET /api/v1/admin/reservations`：核销单分页查询，参数 `page`/`size`/`status`/`verifyCode`/`keyword`/`startDate`/`endDate`；`keyword` 匹配预约单号、核销码、顾客手机号/昵称。
- `GET /api/v1/admin/reservations/{id}`：核销单详情，跨商户访问返回 `40401`。
- `POST /api/v1/admin/reservations/verify`：凭核销码核销。

请求：

```json
{ "verifyCode": "773475" }
```

响应（节选）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reservationId": 15,
    "reservationNo": "R20261125032854830368",
    "verifyCode": "773475",
    "status": "COMPLETED",
    "customerId": 11,
    "customerPhone": "13900009999",
    "appointmentTime": "2026-11-26 09:00:00",
    "serviceItemName": "头疗",
    "resourceName": "美容师A",
    "verifiedAt": "2026-11-25 03:28:58",
    "verifiedBy": 1
  }
}
```

伪代码（ReservationService.verify）：

```text
1. merchantId/operatorId 取自登录态，核销码只在「本商户」内查找
2. 查不到 → 40401「核销码不存在」
3. 状态不是 CONFIRMED → 40902，按状态给出具体文案（未支付/已核销/已取消等）
4. UPDATE reservation SET status='COMPLETED', verified_at=NOW(), verified_by=?
   WHERE merchant_id=? AND verify_code=? AND status='CONFIRMED'
5. 影响行数=0 → 并发下已被其他终端核销，重新查询后返回 40902「该预约已核销」
6. 写 audit_log（action=VERIFY_RESERVATION），返回核销结果
```

- 核销码在创建预约时生成（6 位数字，商户内唯一，唯一索引 `uk_merchant_verify_code`）；普通预约支付成功转 `CONFIRMED` 后才对顾客可见。
- 历史预约由迁移 `V12__backfill_reservation_verify_code.sql` 按 id 确定性补码，保证老数据同样可核销。
- 状态条件更新天然互斥，可防止重复核销与并发竞争。

## 9. 鉴权与数据隔离

- 登录：Sa-Token，token 放 Authorization 头；顾客/员工分登录接口。
- 注解：@SaCheckLogin、@SaCheckRole("ADMIN")。
- 数据隔离：service 层所有查询强制带 merchant_id（从登录态或租户上下文取），防止越权访问他商户数据。

## 10. 定时任务

| 任务 | 频率 | 说明 |
|---|---|---|
| 超时释放 | 每分钟 | 见 8.4 |
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
