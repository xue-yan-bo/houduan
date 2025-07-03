# ===================================================================
# Dockerfile for Homework Service
# Spring Cloud Alibaba 2023 最佳实践 Docker 配置
# ===================================================================

# 使用官方OpenJDK 21镜像作为基础镜像
FROM openjdk:21-jdk-slim

# 设置维护者信息
LABEL maintainer="JLM Team <team@jlm.com>"
LABEL version="25.0"
LABEL description="JLM Homework Service - Spring Cloud Alibaba 2023"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用目录
WORKDIR /app

# 创建非root用户
RUN groupadd -r jlm && useradd -r -g jlm jlm

# 创建日志目录
RUN mkdir -p /var/log/homework && chown -R jlm:jlm /var/log/homework

# 复制jar文件
COPY target/homework-25.0.jar app.jar

# 设置文件权限
RUN chown jlm:jlm app.jar

# 切换到非root用户
USER jlm

# 暴露端口
EXPOSE 18080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:18080/actuator/health || exit 1

# JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/homework/"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
