package com.jlm.homework.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jlm.homework.config.EnvLoader
import com.jlm.homework.config.TestConfiguration
import com.jlm.homework.dto.ExerciseBookRequest
import com.jlm.homework.entity.ExerciseBookStatus
import com.jlm.homework.service.ExerciseBookServer
import com.jlm.homework.service.UserService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.Mockito.*

/**
 * 练习册控制器测试
 */
@WebMvcTest(ExerciseBookController::class)
@ActiveProfiles("test")
@Import(TestConfiguration::class)
class ExerciseBookControllerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupClass() {
            EnvLoader.initTestEnvironment()
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var exerciseBookService: ExerciseBookServer

    @MockBean
    private lateinit var userService: UserService

    @Test
    fun `测试创建练习册 - 成功`() {
        // 准备测试数据
        val request = ExerciseBookRequest(
            title = "测试练习册",
            description = "这是一个测试练习册",
            subject = "数学",
            subjectId = 1L,
            grade = "三年级",
            gradeId = 3L,
            classId = 101L,
            difficultyLevel = 2
        )

        // Mock 用户服务返回用户ID
        `when`(userService.getCurrentUserId()).thenReturn(1001L)

        // Mock 练习册服务保存方法
        val savedEntity = request.toEntity(1001L).copy(id = 1L)
        `when`(exerciseBookService.save(any())).thenReturn(savedEntity)

        // 执行请求
        mockMvc.perform(
            post("/api/exercise-book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("测试练习册"))
            .andExpect(jsonPath("$.data.creatorId").value(1001))

        // 验证方法调用
        verify(userService).getCurrentUserId()
        verify(exerciseBookService).save(any())
    }

    @Test
    fun `测试创建练习册 - 标题为空`() {
        val request = ExerciseBookRequest(
            title = "",
            description = "描述"
        )

        mockMvc.perform(
            post("/api/exercise-book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("练习册标题不能为空"))
    }

    @Test
    fun `测试创建练习册 - 无法获取用户信息`() {
        val request = ExerciseBookRequest(
            title = "测试练习册"
        )

        // Mock 用户服务返回null
        `when`(userService.getCurrentUserId()).thenReturn(null)

        mockMvc.perform(
            post("/api/exercise-book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("无法获取当前用户信息，请重新登录"))
    }
}
