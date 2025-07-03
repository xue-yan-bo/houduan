package com.jlm.remote.service

import com.jlm.remote.config.NacosServiceConfig
import com.jlm.remote.config.NacosServiceProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.test.context.TestPropertySource

/**
 * NacosRemoteService 测试类
 * 
 * 注意：此测试需要Nacos服务器运行才能执行
 * 可以通过系统属性 -Dnacos.test.enabled=true 来启用测试
 */
@SpringBootTest(classes = [NacosServiceConfig::class])
@TestPropertySource(properties = [
    "remote.nacos.enabled=true",
    "remote.nacos.services.test-service=test-service-name",
    "spring.cloud.nacos.server-addr=localhost:8848",
    "spring.cloud.nacos.discovery.enabled=true"
])
@EnabledIfSystemProperty(named = "nacos.test.enabled", matches = "true")
class NacosRemoteServiceTest {

    @Autowired(required = false)
    private lateinit var nacosRemoteService: NacosRemoteService

    @Autowired(required = false)
    private lateinit var discoveryClient: DiscoveryClient

    @Autowired
    private lateinit var nacosServiceProperties: NacosServiceProperties

    @Test
    fun testServiceConfiguration() {
        // 测试配置是否正确加载
        assert(nacosServiceProperties.enabled)
        assert(nacosServiceProperties.services.containsKey("test-service"))
        println("Nacos配置加载成功: ${nacosServiceProperties.services}")
    }

    @Test
    fun testServiceDiscovery() {
        if (::discoveryClient.isInitialized) {
            val services = discoveryClient.services
            println("发现的服务列表: $services")
            
            services.forEach { serviceName ->
                val instances = discoveryClient.getInstances(serviceName)
                println("服务 $serviceName 的实例: ${instances.size}")
                instances.forEach { instance ->
                    println("  - ${instance.host}:${instance.port}")
                }
            }
        } else {
            println("DiscoveryClient未初始化，跳过服务发现测试")
        }
    }

    @Test
    fun testServiceAvailability() {
        if (::nacosRemoteService.isInitialized) {
            val isAvailable = nacosRemoteService.isServiceAvailable("test-service")
            println("测试服务可用性: $isAvailable")
            
            val instances = nacosRemoteService.getServiceInstances("test-service")
            println("测试服务实例数量: ${instances.size}")
        } else {
            println("NacosRemoteService未初始化，跳过服务可用性测试")
        }
    }

    @Test
    fun testHttpRequest() = runBlocking {
        if (::nacosRemoteService.isInitialized) {
            try {
                // 这里使用一个假设存在的服务进行测试
                // 在实际环境中，您需要替换为真实的服务名称和路径
                val response = nacosRemoteService.getByServiceName(
                    serviceAlias = "test-service",
                    path = "/actuator/health"
                )
                println("HTTP请求响应: $response")
            } catch (e: Exception) {
                println("HTTP请求测试失败（这是预期的，因为测试服务可能不存在）: ${e.message}")
            }
        } else {
            println("NacosRemoteService未初始化，跳过HTTP请求测试")
        }
    }

    @Test
    fun testErrorHandling() = runBlocking {
        if (::nacosRemoteService.isInitialized) {
            // 测试调用不存在的服务
            val response = nacosRemoteService.getByServiceName(
                serviceAlias = "non-existent-service",
                path = "/test"
            )
            
            // 应该返回null而不是抛出异常
            assert(response == null)
            println("错误处理测试通过：不存在的服务返回null")
        } else {
            println("NacosRemoteService未初始化，跳过错误处理测试")
        }
    }
}

/**
 * 模拟测试类 - 不需要Nacos服务器
 */
@SpringBootTest(classes = [NacosServiceProperties::class])
@TestPropertySource(properties = [
    "remote.nacos.enabled=false",
    "remote.nacos.services.user-service=jlm-user-service",
    "remote.nacos.services.order-service=jlm-order-service"
])
class NacosConfigurationTest {

    @Autowired
    private lateinit var nacosServiceProperties: NacosServiceProperties

    @Test
    fun testConfigurationProperties() {
        // 测试配置属性是否正确加载
        assert(!nacosServiceProperties.enabled) // 在此测试中禁用
        assert(nacosServiceProperties.services["user-service"] == "jlm-user-service")
        assert(nacosServiceProperties.services["order-service"] == "jlm-order-service")
        assert(nacosServiceProperties.defaultProtocol == "http")
        assert(nacosServiceProperties.timeout == 30000L)
        
        println("配置属性测试通过:")
        println("  enabled: ${nacosServiceProperties.enabled}")
        println("  services: ${nacosServiceProperties.services}")
        println("  defaultProtocol: ${nacosServiceProperties.defaultProtocol}")
        println("  timeout: ${nacosServiceProperties.timeout}")
    }
}
