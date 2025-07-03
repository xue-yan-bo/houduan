package com.jlm.remote.service

import com.jlm.remote.config.NacosServiceProperties
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * 基于Nacos的远程服务调用类
 * 提供通过服务名称调用远程接口的功能，支持负载均衡
 */
@Service
@ConditionalOnClass(name = ["com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient"])
@ConditionalOnProperty(prefix = "remote.nacos", name = ["enabled"], havingValue = "true")
@ConditionalOnBean(name = ["nacosWebClient"])
class NacosRemoteService(
    @Qualifier("nacosWebClient") private val nacosWebClient: WebClient,
    private val discoveryClient: DiscoveryClient,
    private val nacosServiceProperties: NacosServiceProperties
) {

    private val logger = LoggerFactory.getLogger(NacosRemoteService::class.java)

    init {
        logger.info("NacosRemoteService 正在初始化...")
        logger.info("配置的服务映射: {}", nacosServiceProperties.services)
        logger.info("Nacos 功能已启用: {}", nacosServiceProperties.enabled)
    }

    /**
     * 通过服务名称发送GET请求
     * @param serviceAlias 服务别名（在配置文件中定义）
     * @param path 请求路径
     * @param headers 请求头
     * @return 响应字符串
     */
    suspend fun getByServiceName(
        serviceAlias: String, 
        path: String, 
        headers: Map<String, String> = emptyMap()
    ): String? {
        val serviceName = getActualServiceName(serviceAlias)
        if (serviceName == null) {
            logger.error("未找到服务别名对应的服务名称: $serviceAlias")
            return null
        }

        return try {
            val url = buildServiceUrl(serviceName, path)
            logger.debug("发送GET请求到服务: $url")
            
            nacosWebClient.get()
                .uri(url)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingleOrNull()
        } catch (e: Exception) {
            logger.error("通过服务名称发送GET请求失败: $serviceAlias -> $path", e)
            null
        }
    }

    /**
     * 通过服务名称发送GET请求并返回指定类型对象
     */
    suspend fun <T> getObjectByServiceName(
        serviceAlias: String,
        path: String,
        clazz: Class<T>,
        headers: Map<String, String> = emptyMap()
    ): T? {
        val serviceName = getActualServiceName(serviceAlias)
        if (serviceName == null) {
            logger.error("未找到服务别名对应的服务名称: $serviceAlias")
            return null
        }

        return try {
            val url = buildServiceUrl(serviceName, path)
            logger.debug("发送GET请求到服务: $url")
            
            nacosWebClient.get()
                .uri(url)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .retrieve()
                .bodyToMono(clazz)
                .awaitSingleOrNull()
        } catch (e: Exception) {
            logger.error("通过服务名称发送GET请求失败: $serviceAlias -> $path", e)
            null
        }
    }

    /**
     * 通过服务名称发送POST请求
     */
    suspend fun <T> postByServiceName(
        serviceAlias: String,
        path: String,
        body: Any,
        clazz: Class<T>,
        headers: Map<String, String> = emptyMap()
    ): T? {
        val serviceName = getActualServiceName(serviceAlias)
        if (serviceName == null) {
            logger.error("未找到服务别名对应的服务名称: $serviceAlias")
            return null
        }

        return try {
            val url = buildServiceUrl(serviceName, path)
            logger.debug("发送POST请求到服务: $url")
            
            nacosWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .bodyValue(body)
                .retrieve()
                .bodyToMono(clazz)
                .awaitSingleOrNull()
        } catch (e: Exception) {
            logger.error("通过服务名称发送POST请求失败: $serviceAlias -> $path", e)
            null
        }
    }

    /**
     * 获取服务的所有实例
     */
    fun getServiceInstances(serviceAlias: String): List<ServiceInstance> {
        val serviceName = getActualServiceName(serviceAlias)
        return if (serviceName != null) {
            try {
                discoveryClient.getInstances(serviceName)
            } catch (e: Exception) {
                logger.error("获取服务实例失败: $serviceName", e)
                emptyList()
            }
        } else {
            logger.error("未找到服务别名对应的服务名称: $serviceAlias")
            emptyList()
        }
    }

    /**
     * 检查服务是否可用
     */
    fun isServiceAvailable(serviceAlias: String): Boolean {
        return getServiceInstances(serviceAlias).isNotEmpty()
    }

    /**
     * 获取实际的服务名称
     */
    private fun getActualServiceName(serviceAlias: String): String? {
        return nacosServiceProperties.services[serviceAlias] ?: serviceAlias.takeIf { 
            // 如果没有在配置中找到，尝试直接使用别名作为服务名称
            logger.debug("使用服务别名作为实际服务名称: $serviceAlias")
            true
        }
    }

    /**
     * 构建服务URL
     */
    private fun buildServiceUrl(serviceName: String, path: String): String {
        val protocol = nacosServiceProperties.defaultProtocol
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$protocol://$serviceName$cleanPath"
    }
}
