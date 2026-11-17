# ============================================================
# 多阶段构建：先用 Maven 编译打包，再把产物拷贝到运行镜像
# 前端同学可类比为：build 阶段（npm run build 打包） + 部署阶段（用 nginx 托管产物）
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先只拷贝 pom.xml 单独下载依赖，利用 Docker 层缓存（之后改代码不用重新下载依赖）
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 再拷贝源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
# 从构建阶段拷贝可执行 jar
COPY --from=build /app/target/*.jar app.jar
# 设置 JVM 时区为上海
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
