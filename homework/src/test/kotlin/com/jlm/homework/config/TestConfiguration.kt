package com.jlm.homework.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import com.jlm.homework.service.UserService
import com.jlm.homework.feign.SystemFeignClient
import com.jlm.homework.feign.TeacherFeignClient
import com.jlm.homework.feign.LoginUserInfo
import com.jlm.homework.feign.UserInfo
import com.jlm.homework.feign.Teacher
import org.mockito.Mockito

/**
 * 测试配置类
 * 提供测试环境下的Mock Bean
 */
@TestConfiguration
@Profile("test")
class TestConfiguration {

    /**
     * Mock SystemFeignClient for testing
     */
    @Bean
    @Primary
    fun mockSystemFeignClient(): SystemFeignClient {
        val mock = Mockito.mock(SystemFeignClient::class.java)
        
        // 模拟返回用户信息
        val mockUserInfo = UserInfo(
            admin = false,
            userName = "testuser",
            nickName = "测试用户",
            userUuid = "test-uuid-123",
            userId = 1001L
        )
        
        val mockLoginUserInfo = LoginUserInfo(
            roles = listOf("ROLE_USER"),
            userInfo = mockUserInfo,
            currentRole = "ROLE_USER"
        )
        
        Mockito.`when`(mock.loginUserInfo()).thenReturn(mockLoginUserInfo)
        
        return mock
    }

    /**
     * Mock TeacherFeignClient for testing
     */
    @Bean
    @Primary
    fun mockTeacherFeignClient(): TeacherFeignClient {
        val mock = Mockito.mock(TeacherFeignClient::class.java)
        
        // 模拟返回教师信息
        val mockTeacher = Teacher(
            userUuid = "test-uuid-123",
            name = "测试教师"
        )
        
        Mockito.`when`(mock.getTeacherByUserUuid("test-uuid-123")).thenReturn(mockTeacher)
        
        return mock
    }
}

/**
 * 环境变量加载工具类
 */
object EnvLoader {
    
    /**
     * 加载.env文件中的环境变量
     */
    fun loadEnvFile() {
        try {
            val envFile = java.io.File(".env")
            if (envFile.exists()) {
                envFile.readLines().forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            System.setProperty(key, value)
                        }
                    }
                }
                println("已加载.env文件中的环境变量")
            } else {
                println("未找到.env文件，跳过环境变量加载")
            }
        } catch (e: Exception) {
            println("加载.env文件失败: ${e.message}")
        }
    }
    
    /**
     * 初始化测试环境
     */
    fun initTestEnvironment() {
        loadEnvFile()
        
        // 设置测试专用的系统属性
        System.setProperty("spring.profiles.active", "test")
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb")
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver")
        System.setProperty("spring.datasource.username", "sa")
        System.setProperty("spring.datasource.password", "")
        
        println("测试环境初始化完成")
    }
}
