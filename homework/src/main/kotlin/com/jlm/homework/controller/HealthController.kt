package com.jlm.homework.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 健康检查控制器
 * 提供服务健康状态、服务发现信息和配置信息
 */
@RestController
@RequestMapping("/api")
class HealthController {

    @Autowired
    private lateinit var discoveryClient: DiscoveryClient

    @Autowired
    private lateinit var environment: Environment


    @Value("\${server.port:18080}")
    private var serverPort: Int = 18080

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    /**
     * 健康检查接口
     * GET /api/health
     */
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now().toString(),
            "application" to "homework",
            "port" to serverPort,
            "profile" to environment.activeProfiles.joinToString(",").ifEmpty { "default" },
            "message" to "Homework服务运行正常"
        )
    }

    /**
     * 服务发现信息接口
     * GET /api/discovery
     */
    @GetMapping("/discovery")
    fun discovery(): Map<String, Any> {
        // 获取所有已发现的服务
        val services = discoveryClient.services
        
        // 获取当前服务的实例信息
        val instances = mutableListOf<ServiceInstance>()
        services.forEach { serviceName ->
            instances.addAll(discoveryClient.getInstances(serviceName))
        }
        
        return mapOf(
            "discoveredServices" to services,
            "currentServiceInstances" to instances,
            "message" to "服务发现信息获取成功"
        )
    }

    /**
     * 配置信息接口
     * GET /api/config
     */
    @GetMapping("/config")
    fun config(): Map<String, Any> {
        return mapOf(
            "activeProfiles" to environment.activeProfiles.toList(),
            "serverPort" to serverPort,
            "applicationName" to (environment.getProperty("spring.application.name") ?: "homework"),
            "nacosServerAddr" to (environment.getProperty("spring.cloud.nacos.server-addr") ?: "未配置"),
            "discoveryEnabled" to (environment.getProperty("spring.cloud.nacos.discovery.enabled") ?: "false"),
            "configEnabled" to (environment.getProperty("spring.cloud.nacos.config.enabled") ?: "false"),
            "remoteNacosEnabled" to (environment.getProperty("remote.nacos.enabled") ?: "false"),
            "message" to "配置信息获取成功"
        )
    }


    /**
     * Bean状态检查接口
     * GET /api/beans-status
     */
    @GetMapping("/beans-status")
    fun beansStatus(): Map<String, Any> {
        return mapOf(
            "nacosWebClient" to applicationContext.containsBean("nacosWebClient"),
            "discoveryClient" to applicationContext.containsBean("discoveryClient"),
            "nacosRemoteService" to applicationContext.containsBean("nacosRemoteService"),
            "nacosConfigDiagnosticService" to applicationContext.containsBean("nacosConfigDiagnosticService"),
            "nacosServiceConfig" to applicationContext.containsBean("nacosServiceConfig"),
            "nacosServiceProperties" to applicationContext.containsBean("nacosServiceProperties"),
            "remoteServerAutoConfiguration" to applicationContext.containsBean("remoteServerAutoConfiguration"),
            "message" to "Bean状态检查完成"
        )
    }
} 