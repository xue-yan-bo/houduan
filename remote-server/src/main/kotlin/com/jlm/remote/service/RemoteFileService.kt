package com.jlm.remote.service

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * 远程文件服务
 * 提供文件下载、上传等功能
 */
@Service
class RemoteFileService(
    @Qualifier("defaultWebClient") private val webClient: WebClient
) {
    
    private val logger = LoggerFactory.getLogger(RemoteFileService::class.java)

    /**
     * 下载文件到字节数组
     */
    suspend fun downloadFileToBytes(url: String): ByteArray? {
        return try {
            val dataBufferFlux: Flux<DataBuffer> = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(DataBuffer::class.java)

            val outputStream = ByteArrayOutputStream()
            dataBufferFlux.collectList().awaitSingleOrNull()?.forEach { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                outputStream.write(bytes)
            }
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("下载文件失败: $url", e)
            null
        }
    }

    /**
     * 下载文件到本地路径
     */
    suspend fun downloadFileToPath(url: String, targetPath: Path): Boolean {
        return try {
            val bytes = downloadFileToBytes(url)
            if (bytes != null) {
                Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("下载文件到路径失败: $url -> $targetPath", e)
            false
        }
    }

    /**
     * 获取文件信息（不下载内容）
     */
    suspend fun getFileInfo(url: String): FileInfo? {
        return try {
            val response = webClient.head()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .awaitSingleOrNull()

            response?.let {
                FileInfo(
                    contentLength = it.headers.contentLength,
                    contentType = it.headers.contentType?.toString(),
                    lastModified = it.headers.lastModified
                )
            }
        } catch (e: Exception) {
            logger.error("获取文件信息失败: $url", e)
            null
        }
    }

    /**
     * 检查文件是否存在
     */
    suspend fun fileExists(url: String): Boolean {
        return try {
            webClient.head()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .awaitSingleOrNull() != null
        } catch (e: Exception) {
            logger.debug("文件不存在: $url")
            false
        }
    }
}

/**
 * 文件信息数据类
 */
data class FileInfo(
    val contentLength: Long,
    val contentType: String?,
    val lastModified: Long
)
