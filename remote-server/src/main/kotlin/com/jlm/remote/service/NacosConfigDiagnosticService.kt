package com.jlm.remote.service

import com.jlm.remote.config.NacosServiceProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Nacos配置诊断服务
 * 用于诊断Nacos相关配置和Bean的初始化状态
 */
@Service
@ConditionalOnProperty(prefix = "remote.nacos", name = ["enabled"], havingValue = "true")
class NacosConfigDiagnosticService {

    private val logger = LoggerFactory.getLogger(NacosConfigDiagnosticService::class.java)

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired(required = false)
    private var nacosServiceProperties: NacosServiceProperties? = null

    @Autowired(required = false)
    private var discoveryClient: DiscoveryClient? = null

    @EventListener(ApplicationReadyEvent::class)
    fun diagnoseNacosConfiguration() {
        logger.info("=== Nacos 配置诊断开始 ===")

        // 检查配置属性
        if (nacosServiceProperties != null) {
            logger.info("✓ NacosServiceProperties 已加载")
            logger.info("  - enabled: {}", nacosServiceProperties!!.enabled)
            logger.info("  - services: {}", nacosServiceProperties!!.services)
            logger.info("  - defaultProtocol: {}", nacosServiceProperties!!.defaultProtocol)
            logger.info("  - timeout: {}", nacosServiceProperties!!.timeout)
        } else {
            logger.warn("✗ NacosServiceProperties 未加载")
        }

        // 检查 DiscoveryClient
        if (discoveryClient != null) {
            logger.info("✓ DiscoveryClient 已初始化: {}", discoveryClient!!.javaClass.simpleName)
            try {
                val services = discoveryClient!!.services
                logger.info("  - 发现的服务数量: {}", services.size)
                if (services.isNotEmpty()) {
                    logger.info("  - 服务列表: {}", services)
                }
            } catch (e: Exception) {
                logger.warn("  - 获取服务列表失败: {}", e.message)
            }
        } else {
            logger.warn("✗ DiscoveryClient 未初始化")
        }

        // 检查 nacosWebClient Bean
        val hasNacosWebClient = applicationContext.containsBean("nacosWebClient")
        if (hasNacosWebClient) {
            logger.info("✓ nacosWebClient Bean 已创建")
        } else {
            logger.warn("✗ nacosWebClient Bean 未创建")
        }

        // 检查 NacosRemoteService Bean
        val hasNacosRemoteService = applicationContext.containsBean("nacosRemoteService")
        if (hasNacosRemoteService) {
            logger.info("✓ NacosRemoteService Bean 已创建")
        } else {
            logger.warn("✗ NacosRemoteService Bean 未创建")
        }

        // 检查 Nacos 相关的类是否在 classpath 中
        try {
            Class.forName("com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient")
            logger.info("✓ NacosDiscoveryClient 类在 classpath 中")
        } catch (e: ClassNotFoundException) {
            logger.warn("✗ NacosDiscoveryClient 类不在 classpath 中")
        }

        try {
            Class.forName("org.springframework.cloud.client.loadbalancer.LoadBalanced")
            logger.info("✓ LoadBalanced 注解在 classpath 中")
        } catch (e: ClassNotFoundException) {
            logger.warn("✗ LoadBalanced 注解不在 classpath 中")
        }

        logger.info("=== Nacos 配置诊断结束 ===")
    }

    /**
     * 获取诊断报告
     */
    fun getDiagnosticReport(): Map<String, Any> {
        val report = mutableMapOf<String, Any>()

        // 配置属性状态
        report["nacosServicePropertiesLoaded"] = nacosServiceProperties != null
        if (nacosServiceProperties != null) {
            report["nacosServiceProperties"] = mapOf(
                "enabled" to nacosServiceProperties!!.enabled,
                "servicesCount" to nacosServiceProperties!!.services.size,
                "services" to nacosServiceProperties!!.services,
                "defaultProtocol" to nacosServiceProperties!!.defaultProtocol,
                "timeout" to nacosServiceProperties!!.timeout
            )
        }

        // DiscoveryClient 状态
        report["discoveryClientAvailable"] = discoveryClient != null
        if (discoveryClient != null) {
            try {
                val services = discoveryClient!!.services
                report["discoveredServices"] = services
                report["discoveredServicesCount"] = services.size
            } catch (e: Exception) {
                report["discoveryClientError"] = e.message ?: "Unknown error"
            }
        }

        // Bean 状态
        report["nacosWebClientBeanExists"] = applicationContext.containsBean("nacosWebClient")
        report["nacosRemoteServiceBeanExists"] = applicationContext.containsBean("nacosRemoteService")

        // 类路径检查
        report["nacosDiscoveryClientInClasspath"] = try {
            Class.forName("com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        report["loadBalancedInClasspath"] = try {
            Class.forName("org.springframework.cloud.client.loadbalancer.LoadBalanced")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        return report
    }
}
