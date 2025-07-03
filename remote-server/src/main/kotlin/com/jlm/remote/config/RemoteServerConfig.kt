package com.jlm.remote.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Remote Server 配置类
 * 提供远程服务相关的Bean配置
 */
@Configuration
class RemoteServerConfig {

    /**
     * 默认的WebClient配置
     */
    @Bean("defaultWebClient")
    fun defaultWebClient(): WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
            }
            .build()
    }

    /**
     * 带超时配置的WebClient
     */
    @Bean("timeoutWebClient")
    fun timeoutWebClient(): WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
            }
            .build()
    }
}
