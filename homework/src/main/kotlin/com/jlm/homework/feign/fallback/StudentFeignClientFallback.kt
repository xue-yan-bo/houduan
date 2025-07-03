package com.jlm.homework.feign.fallback

import com.jlm.homework.feign.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 学生服务 Feign 客户端降级处理
 * 当服务调用失败时提供默认响应
 */
@Component
class StudentFeignClientFallback : StudentFeignClient {

    private val logger = LoggerFactory.getLogger(StudentFeignClientFallback::class.java)

    override fun getStudentById(studentId: String): StudentResponse? {
        logger.warn("学生服务不可用，使用降级处理 - getStudentById: {}", studentId)
        return StudentResponse(
            success = false,
            message = "学生服务暂时不可用，请稍后重试",
            data = null
        )
    }

    override fun getStudentList(page: Int, size: Int): StudentListResponse? {
        logger.warn("学生服务不可用，使用降级处理 - getStudentList: page={}, size={}", page, size)
        return StudentListResponse(
            success = false,
            message = "学生服务暂时不可用，请稍后重试",
            data = StudentListData(
                students = emptyList(),
                total = 0,
                page = page,
                size = size,
                totalPages = 0
            )
        )
    }

    override fun createStudent(studentRequest: CreateStudentRequest): StudentResponse? {
        logger.warn("学生服务不可用，使用降级处理 - createStudent: {}", studentRequest.name)
        return StudentResponse(
            success = false,
            message = "学生服务暂时不可用，无法创建学生，请稍后重试",
            data = null
        )
    }

    override fun updateStudent(studentId: String, studentRequest: UpdateStudentRequest): StudentResponse? {
        logger.warn("学生服务不可用，使用降级处理 - updateStudent: {}", studentId)
        return StudentResponse(
            success = false,
            message = "学生服务暂时不可用，无法更新学生信息，请稍后重试",
            data = null
        )
    }

    override fun deleteStudent(studentId: String): ApiResponse? {
        logger.warn("学生服务不可用，使用降级处理 - deleteStudent: {}", studentId)
        return ApiResponse(
            success = false,
            message = "学生服务暂时不可用，无法删除学生，请稍后重试"
        )
    }

    override fun healthCheck(): Map<String, Any>? {
        logger.warn("学生服务不可用，使用降级处理 - healthCheck")
        return mapOf(
            "status" to "DOWN",
            "message" to "学生服务不可用",
            "timestamp" to System.currentTimeMillis()
        )
    }
}
