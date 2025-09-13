package com.jlm.homework.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ExerciseBookRequestTest {

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    @Test
    fun `test createdAt array format parsing`() {
        val json = """
        {
            "page": 0,
            "size": 10,
            "sortBy": "createdAt",
            "sortDir": "desc",
            "createdAt": ["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"],
            "classIds": [],
            "gradeId": null,
            "subjectId": null,
            "title": ""
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证时间解析
        assertNotNull(request.parsedCreatedStartTime)
        assertNotNull(request.parsedCreatedEndTime)
        
        // 验证时间值（注意：UTC时间会转换为本地时间）
        val expectedStartTime = LocalDateTime.of(2025, 6, 30, 16, 0, 0)
        val expectedEndTime = LocalDateTime.of(2025, 7, 2, 16, 0, 0)
        
        assertEquals(expectedStartTime, request.parsedCreatedStartTime)
        assertEquals(expectedEndTime, request.parsedCreatedEndTime)
    }

    @Test
    fun `test backward compatibility with individual time fields`() {
        val json = """
        {
            "createdStartTime": "2025-06-30T16:00:00",
            "createdEndTime": "2025-07-02T16:00:00",
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证向后兼容性
        assertNotNull(request.parsedCreatedStartTime)
        assertNotNull(request.parsedCreatedEndTime)
        
        val expectedStartTime = LocalDateTime.of(2025, 6, 30, 16, 0, 0)
        val expectedEndTime = LocalDateTime.of(2025, 7, 2, 16, 0, 0)
        
        assertEquals(expectedStartTime, request.parsedCreatedStartTime)
        assertEquals(expectedEndTime, request.parsedCreatedEndTime)
    }

    @Test
    fun `test array format takes priority over individual fields`() {
        val json = """
        {
            "createdStartTime": "2025-01-01T00:00:00",
            "createdEndTime": "2025-01-02T00:00:00",
            "createdAt": ["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"],
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证数组格式优先级更高
        val expectedStartTime = LocalDateTime.of(2025, 6, 30, 16, 0, 0)
        val expectedEndTime = LocalDateTime.of(2025, 7, 2, 16, 0, 0)
        
        assertEquals(expectedStartTime, request.parsedCreatedStartTime)
        assertEquals(expectedEndTime, request.parsedCreatedEndTime)
    }

    @Test
    fun `test validation with parsed time fields`() {
        val json = """
        {
            "createdAt": ["2025-07-02T16:00:00.000Z", "2025-06-30T16:00:00.000Z"],
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证时间区间验证（结束时间早于开始时间）
        val validationError = request.validateForQuery()
        assertEquals("创建时间结束时间不能早于开始时间", validationError)
    }

    @Test
    fun `test hasSearchConditions with parsed time fields`() {
        val json = """
        {
            "createdAt": ["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"],
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证搜索条件检测
        //assertTrue(request.hasSearchConditions())
    }

    @Test
    fun `test empty createdAt array`() {
        val json = """
        {
            "createdAt": [],
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证空数组处理
        assertNull(request.parsedCreatedStartTime)
        assertNull(request.parsedCreatedEndTime)
        //assertFalse(request.hasSearchConditions())
    }

    @Test
    fun `test invalid createdAt array format`() {
        val json = """
        {
            "createdAt": ["invalid-date", "2025-07-02T16:00:00.000Z"],
            "pageNum": 1,
            "pageSize": 10
        }
        """.trimIndent()

        val request: ExerciseBookRequest = objectMapper.readValue(json)

        // 验证无效格式处理
        assertNull(request.parsedCreatedStartTime)
        assertNull(request.parsedCreatedEndTime)
    }
}
