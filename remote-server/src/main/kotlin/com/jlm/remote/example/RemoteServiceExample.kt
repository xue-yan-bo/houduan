package com.jlm.remote.example

import com.jlm.remote.service.RemoteFileService
import com.jlm.remote.service.RemoteHttpService
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * Remote Server 使用示例
 * 展示如何在其他项目中使用remote-server提供的功能
 */
@Component
class RemoteServiceExample(
    private val remoteHttpService: RemoteHttpService,
    private val remoteFileService: RemoteFileService
) {

    /**
     * HTTP请求示例
     */
    fun httpExample() = runBlocking {
        // GET请求获取字符串
        val response = remoteHttpService.getString("https://api.github.com")
        println("GitHub API响应: ${response?.take(100)}...")

        // 检查URL可访问性
        val isAccessible = remoteHttpService.isUrlAccessible("https://www.google.com")
        println("Google可访问: $isAccessible")

        // POST JSON请求
        val requestData = mapOf("key" to "value", "number" to 42)
        val postResponse = remoteHttpService.postJson(
            "https://httpbin.org/post",
            requestData,
            Map::class.java
        )
        println("POST响应: $postResponse")
    }

    /**
     * 文件操作示例
     */
    fun fileExample() = runBlocking {
        val fileUrl = "https://httpbin.org/robots.txt"
        
        // 检查文件是否存在
        val exists = remoteFileService.fileExists(fileUrl)
        println("文件存在: $exists")

        if (exists) {
            // 获取文件信息
            val fileInfo = remoteFileService.getFileInfo(fileUrl)
            println("文件信息: $fileInfo")

            // 下载文件内容
            val content = remoteFileService.downloadFileToBytes(fileUrl)
            content?.let {
                println("文件内容: ${String(it).take(200)}...")
            }
        }
    }

    /**
     * 综合使用示例
     */
    fun combinedExample() = runBlocking {
        println("=== Remote Server 功能演示 ===")
        
        try {
            httpExample()
            println("\n" + "=".repeat(40) + "\n")
            fileExample()
        } catch (e: Exception) {
            println("示例执行出错: ${e.message}")
        }
    }
}
