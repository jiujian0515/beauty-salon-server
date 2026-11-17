# 后台项目入门指南（写给前端工程师）

> 目标：让你在「边开发边学习」的过程中快速理解这个 Spring Boot 项目的构成与启动原理。

---

## 1. 一句话理解

这个项目就是一个**用 Java 写的后端 API 服务**，对前端暴露 REST 接口，数据存在 MySQL。
你可以把它理解成：一个「帮你读写数据库、处理业务规则」的服务器，前端只负责调接口和展示。

---

## 2. 技术栈速览（与前端类比）

| Java 后端概念 | 前端类比 | 说明 |
|---|---|---|
| Maven / `pom.xml` | npm / `package.json` | 依赖管理 + 构建 |
| `mvn spring-boot:run` | `npm run dev` | 启动开发服务 |
| Spring Boot | Vue / React 框架 | 提供脚手架与运行环境 |
| `@RestController` | 路由 + handler | 处理 HTTP 请求 |
| Service 层 | 业务逻辑函数 | 核心规则与事务 |
| MyBatis-Plus | 一个 ORM / 类似 axios 封装 | 简化数据库增删改查 |
| Flyway | 数据库初始化脚本 | 自动建表 |
| Sa-Token | 登录鉴权中间件 | 登录态 + 权限校验 |
| Lombok | 代码生成插件 | 自动生成 getter/setter |

---

## 3. 目录结构详解

```
beauty-salon-server/
├── pom.xml                        # 依赖清单 + 构建配置（≈ package.json）
├── Dockerfile                     # 把应用打成 Docker 镜像（生产部署用，本地开发用不到）
├── scripts/
│   └── setup-local-db.sql         # 本地一次性初始化数据库脚本
└── src/main/
    ├── java/com/beautysalon/      # 所有 Java 源码
    │   ├── BeautySalonApplication.java  # 启动入口（≈ main.ts）
    │   ├── common/                # 通用：统一响应、错误码、异常处理
    │   │   ├── Result.java        #   统一返回结构 {code, message, data}
    │   │   ├── ErrorCode.java     #   错误码枚举
    │   │   ├── BusinessException.java  # 业务异常
    │   │   └── GlobalExceptionHandler.java # 全局异常兜底
    │   ├── config/                # 配置类（连接数据库、鉴权、跨域等）
    │   │   ├── MybatisPlusConfig.java  # 分页插件
    │   │   ├── SaTokenConfig.java      # 登录鉴权拦截
    │   │   ├── WebConfig.java          # 跨域 CORS
    │   │   └── JacksonConfig.java      # JSON 序列化（Long→String）
    │   ├── controller/            # 接口层（薄，只做参数校验 + 调 service）
    │   ├── service/               # 业务层（核心逻辑 + 事务）
    │   ├── mapper/                # 数据访问层（MyBatis-Plus Mapper）
    │   ├── model/
    │   │   ├── entity/            # 与数据库表一一对应的实体
    │   │   ├── dto/               # 请求/响应对象
    │   │   └── enums/             # 状态枚举
    │   ├── job/                   # 定时任务（超时释放等）
    │   └── integration/           # 第三方集成（支付网关等）
    └── resources/
        ├── application.yml        # 主配置
        ├── application-dev.yml    # 开发环境配置
        ├── application-prod.yml   # 生产环境配置
        └── db/migration/          # Flyway 建表 SQL
            └── V1__init.sql       # 初始化表结构 + 种子数据
```

**分层调用链（一个请求的完整旅程）：**

```
前端请求
   ↓
Controller（接收请求、校验参数）
   ↓
Service（业务逻辑、事务）
   ↓
Mapper（操作数据库）
   ↓
MySQL
```

记住口诀：**Controller 薄、Service 厚、Mapper 只做数据访问。**

---

## 4. 启动流程原理（重点）

当你在命令行执行 `mvn spring-boot:run` 后，发生了这些事：

1. **Maven 编译**：把 `src/main/java` 下的 `.java` 编译成 `.class` 字节码。
2. **执行 main 方法**：`BeautySalonApplication.main()` 被调用。
3. **扫描装配**：Spring 扫描 `com.beautysalon` 包下所有带注解的类，把它们「装配」成一个个 Bean（可理解为全局单例对象）。
4. **读取配置**：加载 `application.yml`（此时 active 的是 dev，所以还会加载 `application-dev.yml`）。
5. **初始化组件**：连接 MySQL、初始化 MyBatis-Plus、Sa-Token、Flyway 等。
6. **Flyway 建表**：自动执行 `db/migration/V1__init.sql`，创建 9 张表 + 写入种子数据。
7. **启动内嵌 Tomcat**：监听 8080 端口，对外提供 HTTP 接口。
8. 浏览器访问 `http://localhost:8080/api/v1/ping` 验证是否成功。

---

## 5. 如何运行（本机步骤）

> 本地开发**不使用 Docker**，直接连接本机已安装的 MySQL 8。

### 第一步：初始化数据库（只需做一次）

用 Navicat / Workbench 等图形工具，或命令行执行项目里的 `scripts/setup-local-db.sql`。
它会自动创建 `beauty_salon` 数据库和专用账号 `beautysalon`（密码 `beautysalon123`）。

PowerShell 命令行方式（执行后会提示输入 root 密码）：

```powershell
Get-Content scripts\setup-local-db.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p
```

> 建表不用手动做：项目启动时 Flyway 会自动执行 `V1__init.sql` 完成建表和种子数据。

### 第二步：启动后端

```bash
# 用 JDK17 启动（你机器上 JDK17 在 D:\Android\jdk17）
mvn spring-boot:run
```

> 如果 `mvn` 报「Unsupported class file major version」，说明 Maven 用的是 JDK11。
> 解决办法：把 `JAVA_HOME` 指向 JDK17，或在 IDE 里把项目 JDK 设为 17。

### 第三步：验证

浏览器访问：`http://localhost:8080/api/v1/ping`

看到 `{"code":0,"message":"success","data":"pong - ..."}` 即成功。

---

## 6. 建议的学习路线

1. **先跑起来**：完成第 5 节，看到 `pong` 返回。
2. **读一个请求的完整链路**：从 `HealthController` 往上追，理解注解和分层。
3. **照着写第一个业务模块**：以「服务项目 CRUD」为例，依次写
   `entity → mapper → service → controller`，这是本项目里最标准的垂直切片。
4. **理解数据库**：打开 `V1__init.sql`，对照 `README-BTD.md` 第 6 节看表结构。
5. **再啃难点**：预约下单的「并发防超卖」、支付回调「幂等」、定时任务「超时释放」。

---

## 7. 常见名词速查

| 名词 | 含义 |
|---|---|
| Bean | 交给 Spring 管理的对象（≈ 全局单例） |
| 依赖注入 @Autowired | 让 Spring 自动把需要的对象传进来 |
| 事务 @Transactional | 一段操作要么全成功、要么全回滚 |
| 注解 | 以 @ 开头的「标记」，告诉框架怎么处理这个类/方法 |
| DTO | 数据传输对象，专门给接口收/发数据用 |
| Entity | 实体，与数据库表字段一一对应 |
