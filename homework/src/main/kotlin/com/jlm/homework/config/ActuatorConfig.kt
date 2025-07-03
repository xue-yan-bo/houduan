package com.jlm.homework.config

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Actuator 配置类
 * 提供健康检查和应用信息
 */
@Configuration
class ActuatorConfig {

    /**
     * 自定义健康检查指示器
     */
    @Bean
    fun customHealthIndicator(): HealthIndicator {
        return HealthIndicator {
            try {
                // 这里可以添加自定义的健康检查逻辑
                // 例如检查数据库连接、外部服务等
                Health.up()
                    .withDetail("status", "服务运行正常")
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build()
            } catch (e: Exception) {
                Health.down()
                    .withDetail("error", e.message)
                    .withException(e)
                    .build()
            }
        }
    }

    /**
     * 自定义应用信息贡献者
     */
    @Bean
    fun customInfoContributor(environment: Environment): InfoContributor {
        return InfoContributor { builder: Info.Builder ->
            builder
                .withDetail("app", mapOf(
                    "name" to "Homework Service",
                    "description" to "JLM Homework 微服务",
                    "version" to "25.0",
                    "profiles" to environment.activeProfiles.toList(),
                    "javaVersion" to System.getProperty("java.version"),
                    "kotlinVersion" to KotlinVersion.CURRENT.toString()
                ))
                .withDetail("build", mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "timezone" to System.getProperty("user.timezone")
                ))
        }
    }
}
