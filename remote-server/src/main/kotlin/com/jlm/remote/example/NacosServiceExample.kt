package com.jlm.remote.example

import com.jlm.remote.service.NacosRemoteService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

/**
 * Nacos服务调用示例
 * 展示如何使用NacosRemoteService通过服务名称调用远程接口
 */
@Component
@ConditionalOnBean(NacosRemoteService::class)
class NacosServiceExample(
    private val nacosRemoteService: NacosRemoteService
) {
    
    private val logger = LoggerFactory.getLogger(NacosServiceExample::class.java)

    /**
     * 基本的服务调用示例
     */
    fun basicServiceCallExample() = runBlocking {
        logger.info("=== Nacos服务调用基本示例 ===")
        
        try {
            // 检查服务是否可用
            val isAvailable = nacosRemoteService.isServiceAvailable("user-service")
            logger.info("用户服务是否可用: $isAvailable")
            
            if (isAvailable) {
                // 获取服务实例信息
                val instances = nacosRemoteService.getServiceInstances("user-service")
                logger.info("用户服务实例数量: ${instances.size}")
                instances.forEach { instance ->
                    logger.info("服务实例: ${instance.host}:${instance.port}")
                }
                
                // 调用用户服务的API
                val userInfo = nacosRemoteService.getByServiceName(
                    serviceAlias = "user-service",
                    path = "/api/user/info",
                    headers = mapOf("Authorization" to "Bearer token123")
                )
                logger.info("用户信息响应: $userInfo")
            }
        } catch (e: Exception) {
            logger.error("基本服务调用示例执行失败", e)
        }
    }

    /**
     * 对象类型响应示例
     */
    fun objectResponseExample() = runBlocking {
        logger.info("=== 对象类型响应示例 ===")
        
        try {
            // 调用返回JSON对象的API
            val response = nacosRemoteService.getObjectByServiceName(
                serviceAlias = "order-service",
                path = "/api/order/list",
                clazz = Map::class.java,
                headers = mapOf("Content-Type" to "application/json")
            )
            
            logger.info("订单列表响应: $response")
        } catch (e: Exception) {
            logger.error("对象类型响应示例执行失败", e)
        }
    }

    /**
     * POST请求示例
     */
    fun postRequestExample() = runBlocking {
        logger.info("=== POST请求示例 ===")
        
        try {
            // 构建请求数据
            val requestData = mapOf(
                "name" to "测试订单",
                "amount" to 100.0,
                "userId" to 12345
            )
            
            // 发送POST请求创建订单
            val createResponse = nacosRemoteService.postByServiceName(
                serviceAlias = "order-service",
                path = "/api/order/create",
                body = requestData,
                clazz = Map::class.java,
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer token123"
                )
            )
            
            logger.info("创建订单响应: $createResponse")
        } catch (e: Exception) {
            logger.error("POST请求示例执行失败", e)
        }
    }

    /**
     * 多服务调用示例
     */
    fun multiServiceExample() = runBlocking {
        logger.info("=== 多服务调用示例 ===")
        
        try {
            // 定义要调用的服务列表
            val services = listOf("user-service", "order-service", "product-service")
            
            services.forEach { serviceAlias ->
                logger.info("检查服务: $serviceAlias")
                
                val isAvailable = nacosRemoteService.isServiceAvailable(serviceAlias)
                if (isAvailable) {
                    val instances = nacosRemoteService.getServiceInstances(serviceAlias)
                    logger.info("$serviceAlias 可用，实例数: ${instances.size}")
                    
                    // 调用健康检查接口
                    val healthCheck = nacosRemoteService.getByServiceName(
                        serviceAlias = serviceAlias,
                        path = "/actuator/health"
                    )
                    logger.info("$serviceAlias 健康状态: $healthCheck")
                } else {
                    logger.warn("$serviceAlias 不可用")
                }
            }
        } catch (e: Exception) {
            logger.error("多服务调用示例执行失败", e)
        }
    }

    /**
     * 错误处理示例
     */
    fun errorHandlingExample() = runBlocking {
        logger.info("=== 错误处理示例 ===")
        
        try {
            // 调用不存在的服务
            val response1 = nacosRemoteService.getByServiceName(
                serviceAlias = "non-existent-service",
                path = "/api/test"
            )
            logger.info("不存在服务的响应: $response1")
            
            // 调用不存在的接口
            val response2 = nacosRemoteService.getByServiceName(
                serviceAlias = "user-service",
                path = "/api/non-existent-endpoint"
            )
            logger.info("不存在接口的响应: $response2")
            
        } catch (e: Exception) {
            logger.error("错误处理示例执行失败", e)
        }
    }

    /**
     * 综合示例演示
     */
    fun runAllExamples() {
        logger.info("开始执行Nacos服务调用示例...")
        
        basicServiceCallExample()
        logger.info("\n" + "=".repeat(50) + "\n")
        
        objectResponseExample()
        logger.info("\n" + "=".repeat(50) + "\n")
        
        postRequestExample()
        logger.info("\n" + "=".repeat(50) + "\n")
        
        multiServiceExample()
        logger.info("\n" + "=".repeat(50) + "\n")
        
        errorHandlingExample()
        
        logger.info("Nacos服务调用示例执行完成")
    }
}
