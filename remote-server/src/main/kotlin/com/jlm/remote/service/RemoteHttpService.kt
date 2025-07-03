package com.jlm.remote.service

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

/**
 * 远程HTTP服务
 * 提供通用的HTTP请求功能
 */
@Service
class RemoteHttpService(
    @Qualifier("defaultWebClient") private val webClient: WebClient
) {
    
    private val logger = LoggerFactory.getLogger(RemoteHttpService::class.java)

    /**
     * GET请求 - 返回字符串
     */
    suspend fun getString(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            webClient.get()
                .uri(url)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .retrieve()
                .bodyToMono<String>()
                .awaitSingleOrNull()
        } catch (e: Exception) {
            logger.error("GET请求失败: $url", e)
            null
        }
    }

    /**
     * GET请求 - 返回指定类型对象
     */
    suspend fun <T> getObject(url: String, clazz: Class<T>, headers: Map<String, String> = emptyMap()): T? {
        return try {
            webClient.get()
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
            logger.error("GET请求失败: $url", e)
            null
        }
    }

    /**
     * POST请求 - JSON数据
     */
    suspend fun <T> postJson(url: String, body: Any, clazz: Class<T>, headers: Map<String, String> = emptyMap()): T? {
        return try {
            webClient.post()
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
            logger.error("POST请求失败: $url", e)
            null
        }
    }

    /**
     * 检查URL是否可访问
     */
    suspend fun isUrlAccessible(url: String): Boolean {
        return try {
            webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
            true
        } catch (e: Exception) {
            logger.debug("URL不可访问: $url")
            false
        }
    }
}
