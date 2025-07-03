package com.jlm.remote

import com.jlm.remote.config.NacosServiceConfig
import com.jlm.remote.config.RemoteServerConfig
import com.jlm.remote.service.NacosConfigDiagnosticService
import com.jlm.remote.service.NacosRemoteService
import com.jlm.remote.service.RemoteFileService
import com.jlm.remote.service.RemoteHttpService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Remote Server 自动配置类
 * 当其他Spring Boot项目引入此jar时，会自动配置相关Bean
 */
@Configuration
@Import(
    RemoteServerConfig::class,
    RemoteHttpService::class,
    RemoteFileService::class,
    NacosServiceConfig::class,
    NacosRemoteService::class,
    NacosConfigDiagnosticService::class
)
class RemoteServerAutoConfiguration
