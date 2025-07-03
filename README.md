# JLM Server 25 - Spring Cloud Alibaba 2023 微服务架构

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://openjdk.java.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org/)

## 项目简介

JLM Server 25 是基于 Spring Cloud Alibaba 2023 版本构建的现代化微服务架构项目，采用最新的技术栈和最佳实践，提供高性能、高可用、易扩展的微服务解决方案。

## 技术栈

- **框架**: Spring Boot 3.2.4, Spring Cloud 2023.0.1
- **微服务**: Spring Cloud Alibaba 2023.0.1.0
- **服务发现**: Nacos 2.3.2
- **负载均衡**: Spring Cloud LoadBalancer
- **编程语言**: Java 21, Kotlin 1.9.25
- **构建工具**: Maven 3.9+
- **容器化**: Docker, Docker Compose

## 项目结构

```
jlm-server25/
├── homework/                    # 业务应用模块
│   ├── src/main/kotlin/        # Kotlin源码
│   ├── src/main/resources/     # 配置文件
│   ├── Dockerfile              # Docker构建文件
│   └── pom.xml                 # Maven配置
├── remote-server/              # 远程调用库模块
│   ├── src/main/kotlin/        # 远程调用核心代码
│   ├── src/main/resources/     # 库配置文件
│   └── pom.xml                 # Maven配置
├── scripts/                    # 启动停止脚本
│   ├── start.sh               # 启动脚本
│   └── stop.sh                # 停止脚本
├── docs/                      # 项目文档
├── docker-compose.yml         # Docker编排配置
└── pom.xml                    # 父级Maven配置
```

## 核心特性

### 🚀 现代化架构
- 基于 Spring Boot 3.x 和 Java 21
- 响应式编程支持 (WebFlux)
- Kotlin 协程集成

### 🔧 服务治理
- Nacos 服务发现与配置管理
- 智能负载均衡
- 服务健康检查
- 优雅启停

### 📊 可观测性
- Spring Boot Actuator 监控
- Prometheus 指标收集
- 结构化日志输出
- 分布式链路追踪

### 🛡️ 生产就绪
- 多环境配置管理
- 容器化部署支持
- 安全最佳实践
- 性能优化配置

## 快速开始

### 环境要求

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (可选)

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd jlm-server25
```

2. **启动服务**
```bash
# 使用脚本启动 (推荐)
./scripts/start.sh

# 或手动启动
mvn clean package -DskipTests
java -jar homework/target/homework-25.0.jar
```

3. **验证服务**
```bash
# 健康检查
curl http://localhost:18080/actuator/health

# 服务信息
curl http://localhost:18080/actuator/info
```

### Docker 部署

1. **启动完整环境**
```bash
docker-compose up -d
```

2. **查看服务状态**
```bash
docker-compose ps
```

3. **访问服务**
- Homework 服务: http://localhost:18080
- Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)

## 配置说明

### 环境配置

项目支持多环境配置：

- `dev`: 开发环境 (默认)
- `prod`: 生产环境
- `local`: 本地环境 (无Nacos)

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `dev` |
| `NACOS_SERVER_ADDR` | Nacos服务器地址 | `localhost:8848` |
| `NACOS_NAMESPACE` | Nacos命名空间 | `` |
| `NACOS_GROUP` | Nacos配置组 | `DEFAULT_GROUP` |
| `SERVER_PORT` | 服务端口 | `18080` |

## API 文档

### 健康检查接口

- `GET /actuator/health` - 服务健康状态
- `GET /actuator/info` - 服务信息
- `GET /api/health` - 自定义健康检查

### 业务接口

- `GET /api/student/{id}` - 获取学生信息
- `GET /api/student` - 获取学生列表
- `POST /api/student` - 创建学生
- `GET /api/student/status` - 学生服务状态

## 监控和运维

### 监控指标

项目集成了完整的监控体系：

- **应用指标**: JVM、HTTP请求、数据库连接等
- **业务指标**: 自定义业务监控指标
- **基础设施**: 容器、网络、存储等

### 日志管理

- **结构化日志**: JSON格式输出
- **日志级别**: 支持动态调整
- **日志轮转**: 自动归档和清理
- **集中收集**: 支持ELK等日志系统

### 故障排查

1. **查看服务状态**
```bash
curl http://localhost:18080/actuator/health
```

2. **查看配置信息**
```bash
curl http://localhost:18080/api/config
```

3. **查看日志**
```bash
tail -f logs/homework.log
```

## 开发指南

### 代码规范

- 遵循 Kotlin 官方编码规范
- 使用 Spring Boot 最佳实践
- 统一的异常处理和响应格式

### 测试策略

- 单元测试覆盖率 > 80%
- 集成测试覆盖核心业务流程
- 性能测试验证系统容量

### 部署流程

1. 代码提交和审查
2. 自动化测试
3. 构建和打包
4. 部署到测试环境
5. 生产环境发布

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交变更
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系我们

- 项目维护者: JLM Team
- 邮箱: team@jlm.com
- 文档: [Spring Cloud Alibaba 2023 最佳实践](docs/SPRING_CLOUD_ALIBABA_2023_BEST_PRACTICES.md)