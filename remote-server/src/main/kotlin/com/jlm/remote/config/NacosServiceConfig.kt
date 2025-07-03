package com.jlm.remote.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Nacos服务配置类
 * 提供基于Nacos服务发现的WebClient配置
 */
@Configuration
@ConditionalOnClass(name = ["com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient"])
@ConditionalOnProperty(prefix = "remote.nacos", name = ["enabled"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(NacosServiceProperties::class)
class NacosServiceConfig {

    private val logger = LoggerFactory.getLogger(NacosServiceConfig::class.java)

    init {
        logger.info("NacosServiceConfig 正在初始化...")
    }

    /**
     * 支持负载均衡的WebClient
     * 用于通过服务名称调用远程服务
     */
    @Bean("nacosWebClient")
    @LoadBalanced
    @ConditionalOnBean(DiscoveryClient::class)
    fun nacosWebClient(): WebClient {
        logger.info("创建 nacosWebClient Bean...")
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
            }
            .build()
    }
}

/**
 * Nacos服务配置属性
 */
@ConfigurationProperties(prefix = "remote.nacos")
data class NacosServiceProperties(
    /**
     * 是否启用Nacos服务发现功能
     */
    var enabled: Boolean = false,
    
    /**
     * 服务配置映射
     * key: 服务别名, value: 实际的Nacos服务名称
     */
    var services: Map<String, String> = emptyMap(),
    
    /**
     * 默认的HTTP协议
     */
    var defaultProtocol: String = "http",
    
    /**
     * 请求超时时间（毫秒）
     */
    var timeout: Long = 30000
)
