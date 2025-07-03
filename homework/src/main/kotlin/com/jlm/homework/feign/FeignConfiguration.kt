package com.jlm.homework.feign

import feign.Logger
import feign.Request
import feign.Retryer
import feign.codec.ErrorDecoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.concurrent.TimeUnit

/**
 * Feign 配置类
 * 配置超时、重试、日志等
 */
@Configuration
class FeignConfiguration {

    private val logger = LoggerFactory.getLogger(FeignConfiguration::class.java)

    /**
     * Feign 日志级别配置
     */
    @Bean
    fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.BASIC
    }

    /**
     * 请求超时配置
     */
    @Bean
    fun feignRequestOptions(): Request.Options {
        return Request.Options(
            /* connectTimeout */ 5000,
            /* readTimeout */ 30000,
            /* followRedirects */ true
        )
    }

    /**
     * 重试配置
     */
    @Bean
    fun feignRetryer(): Retryer {
        // 最大重试次数3次，初始间隔100ms，最大间隔1000ms
        return Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3)
    }

    /**
     * 错误解码器
     */
    @Bean
    fun feignErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            logger.warn("Feign调用失败: method={}, status={}, reason={}",
                methodKey, response.status(), response.reason())

            val exception: java.lang.Exception = when (response.status()) {
                400 -> java.lang.IllegalArgumentException("请求参数错误: ${response.reason()}")
                401 -> java.lang.RuntimeException("认证失败: ${response.reason()}")
                403 -> java.lang.RuntimeException("权限不足: ${response.reason()}")
                404 -> java.lang.IllegalArgumentException("资源不存在: ${response.reason()}")
                500 -> java.lang.RuntimeException("服务器内部错误: ${response.reason()}")
                503 -> java.lang.RuntimeException("服务不可用: ${response.reason()}")
                else -> java.lang.RuntimeException("未知错误: ${response.status()} ${response.reason()}")
            }
            exception
        }
    }
}

/**
 * Feign 全局配置
 */
@Configuration
class GlobalFeignConfiguration {

    /**
     * 全局请求拦截器 - 透传认证信息
     */
    @Bean
    fun requestInterceptor(): feign.RequestInterceptor {
        return feign.RequestInterceptor { template ->
            // 添加通用请求头
            template.header("User-Agent", "Homework-Service/1.0")
            template.header("Accept", "application/json")
            template.header("Content-Type", "application/json")

            // 添加请求ID用于链路追踪
            val requestId = generateRequestId()
            template.header("X-Request-ID", requestId)
            template.header("X-Source-Service", "homework")

            // 透传认证信息
            try {
                val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                val request = requestAttributes?.request

                if (request != null) {
                    // 透传 Authorization 头
                    val authorization = request.getHeader("Authorization")
                    if (!authorization.isNullOrEmpty()) {
                        template.header("Authorization", authorization)
                    }

                    // 透传 Admin-Token 头（如果存在）
                    val adminToken = request.getHeader("Admin-Token")
                    if (!adminToken.isNullOrEmpty()) {
                        template.header("Admin-Token", adminToken)
                    }

                    // 透传其他可能的认证头
                    val bearerToken = request.getHeader("Bearer")
                    if (!bearerToken.isNullOrEmpty()) {
                        template.header("Bearer", bearerToken)
                    }
                }
            } catch (e: Exception) {
                // 如果获取请求上下文失败，记录日志但不影响请求
                println("警告: 无法获取请求上下文来透传认证信息: ${e.message}")
            }
        }
    }

    private fun generateRequestId(): String {
        return "hw-${System.currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
    }
}
