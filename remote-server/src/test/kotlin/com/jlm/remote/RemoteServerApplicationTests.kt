package com.jlm.remote

import com.jlm.remote.config.RemoteServerConfig
import com.jlm.remote.service.RemoteHttpService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(RemoteServerConfig::class)
class RemoteServerTests {

    @Autowired
    private lateinit var remoteHttpService: RemoteHttpService

    @Test
    fun contextLoads() {
        // 测试配置是否正确加载
        assert(::remoteHttpService.isInitialized)
    }

    @Test
    fun testHttpService() = runBlocking {
        // 测试HTTP服务（使用一个公开的测试API）
        val result = remoteHttpService.isUrlAccessible("https://httpbin.org/get")
        println("URL可访问性测试结果: $result")
    }
}
