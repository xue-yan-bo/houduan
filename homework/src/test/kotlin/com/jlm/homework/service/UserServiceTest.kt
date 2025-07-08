package com.jlm.homework.service

import com.jlm.homework.feign.LoginUserInfo
import com.jlm.homework.feign.SystemFeignClient
import com.jlm.homework.feign.TeacherFeignClient
import com.jlm.homework.feign.UserInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.context.ActiveProfiles

/**
 * UserService 测试类
 */
@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private lateinit var systemFeignClient: SystemFeignClient

    @Mock
    private lateinit var teacherFeignClient: TeacherFeignClient

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `测试获取当前用户ID - 成功`() {
        // 准备测试数据
        val userInfo = UserInfo(
            admin = false,
            userName = "testuser",
            nickName = "测试用户",
            userUuid = "test-uuid",
            userId = 1001L
        )
        val loginUserInfo = LoginUserInfo(
            roles = listOf("ROLE_USER"),
            userInfo = userInfo,
            currentRole = "ROLE_USER"
        )

        // Mock 方法调用
        `when`(systemFeignClient.loginUserInfo()).thenReturn(loginUserInfo)

        // 执行测试
        val result = userService.getCurrentUserId()

        // 验证结果
        assert(result == 1001L) { "应该返回正确的用户ID" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试获取当前用户ID - Feign调用失败`() {
        // Mock 方法抛出异常
        `when`(systemFeignClient.loginUserInfo()).thenThrow(RuntimeException("Feign调用失败"))

        // 执行测试
        val result = userService.getCurrentUserId()

        // 验证结果 - 应该返回默认用户ID
        assert(result == 1000L) { "应该返回默认用户ID" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试获取当前用户ID - 返回数据为空`() {
        // Mock 方法返回null
        `when`(systemFeignClient.loginUserInfo()).thenReturn(null)

        // 执行测试
        val result = userService.getCurrentUserId()

        // 验证结果 - 应该返回默认用户ID
        assert(result == 1000L) { "应该返回默认用户ID" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试获取当前用户ID - 用户信息为空`() {
        // 准备测试数据 - userInfo为null
        val loginUserInfo = LoginUserInfo(
            roles = listOf("ROLE_USER"),
            userInfo = null,
            currentRole = "ROLE_USER"
        )

        // Mock 方法调用
        `when`(systemFeignClient.loginUserInfo()).thenReturn(loginUserInfo)

        // 执行测试
        val result = userService.getCurrentUserId()

        // 验证结果 - 应该返回默认用户ID
        assert(result == 1000L) { "应该返回默认用户ID" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试安全获取当前用户ID - 确保不抛出异常`() {
        // Mock 方法抛出异常
        `when`(systemFeignClient.loginUserInfo()).thenThrow(RuntimeException("网络错误"))

        // 执行测试 - 不应该抛出异常
        val result = userService.getCurrentUserIdSafely()

        // 验证结果
        assert(result == 1000L) { "应该返回默认用户ID" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试获取当前用户信息 - 成功`() {
        // 准备测试数据
        val userInfo = UserInfo(
            admin = false,
            userName = "testuser",
            nickName = "测试用户",
            userUuid = "test-uuid",
            userId = 1001L
        )
        val loginUserInfo = LoginUserInfo(
            roles = listOf("ROLE_USER"),
            userInfo = userInfo,
            currentRole = "ROLE_USER"
        )

        // Mock 方法调用
        `when`(systemFeignClient.loginUserInfo()).thenReturn(loginUserInfo)

        // 执行测试
        val result = userService.getCurrentUserInfo()

        // 验证结果
        assert(result != null) { "应该返回用户信息" }
        assert(result!!.userId == 1001L) { "用户ID应该正确" }
        assert(result.userName == "testuser") { "用户名应该正确" }
        verify(systemFeignClient).loginUserInfo()
    }

    @Test
    fun `测试检查用户是否为教师 - 成功`() {
        // 准备测试数据
        val userInfo = UserInfo(
            admin = false,
            userName = "testuser",
            nickName = "测试用户",
            userUuid = "test-uuid",
            userId = 1001L
        )
        val loginUserInfo = LoginUserInfo(
            roles = listOf("ROLE_USER"),
            userInfo = userInfo,
            currentRole = "ROLE_USER"
        )

        // Mock 方法调用
        `when`(systemFeignClient.loginUserInfo()).thenReturn(loginUserInfo)

        // 执行测试
        val result = userService.isCurrentUserTeacher()

        // 验证结果
        assert(!result) { "应该返回false，因为没有教师信息" }
        verify(systemFeignClient).loginUserInfo()
    }
}
