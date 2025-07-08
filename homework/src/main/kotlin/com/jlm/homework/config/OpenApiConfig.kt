package com.jlm.homework.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3.0 配置类
 * 配置Swagger UI和API文档生成
 */
@Configuration
class OpenApiConfig {

    @Value("\${server.port:18080}")
    private val serverPort: Int = 18080

    @Value("\${spring.application.name:homework}")
    private val applicationName: String = "homework"

    /**
     * 配置OpenAPI基本信息
     */
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$applicationName 服务 API 文档")
                    .description("""
                        ## JLM作业管理系统 - Homework服务API文档
                        
                        ### 服务简介
                        - **服务名称**: $applicationName
                        - **技术栈**: Spring Boot 3.2.4 + Spring Cloud Alibaba 2023 + Kotlin
                        - **数据库**: MySQL + Spring Data JPA
                        - **服务发现**: Nacos
                        - **远程调用**: OpenFeign
                        
                        ### 主要功能模块
                        1. **作业管理** - 作业的创建、查询、更新、删除
                        2. **练习册管理** - 练习册的CRUD操作，支持图片上传
                        3. **健康检查** - 服务状态监控和配置信息查看
                        
                        ### 认证说明
                        - 支持Bearer Token认证：`Authorization: Bearer <token>`
                        - 支持Admin Token认证：`Admin-Token: <admin-token>`
                        - 前端调用时请在请求头中包含认证信息
                        
                        ### 响应格式
                        统一响应格式：
                        ```json
                        {
                          "code": 200,
                          "msg": "操作成功",
                          "data": {},
                          "total": 0,
                          "rows": []
                        }
                        ```
                    """.trimIndent())
                    .version("25.0")
                    .contact(
                        Contact()
                            .name("JLM开发团队")
                            .email("dev@jlm.com")
                            .url("https://jlm.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("本地开发环境"),
                    Server()
                        .url("http://192.168.1.251/homework")
                        .description("Docker容器环境"),
                    Server()
                        .url("https://api.jlm.com")
                        .description("生产环境")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .addSecurityItem(SecurityRequirement().addList("adminToken"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Bearer Token认证，格式：Bearer <token>")
                    )
                    .addSecuritySchemes(
                        "adminToken",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Admin-Token")
                            .description("管理员Token认证")
                    )
            )
    }


}
