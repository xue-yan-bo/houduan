package com.jlm.homework.controller

import com.jlm.homework.entity.HomeworkEntity
import com.jlm.homework.entity.HomeworkStatus
import com.jlm.homework.exception.ResourceNotFoundException
import com.jlm.homework.service.HomeworkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 作业控制器
 * 演示Spring Data JPA的REST API
 */
@Tag(name = "作业管理", description = "作业的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/homework")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "adminToken")
class HomeworkController(
    private val homeworkService: HomeworkService
) {
    
    /**
     * 创建作业
     * POST /api/homework
     */
    @Operation(summary = "创建作业", description = "创建新的作业")
    @PostMapping
    fun createHomework(@RequestBody request: CreateHomeworkRequest): HomeworkEntity {
        val homework = HomeworkEntity(
            title = request.title,
            description = request.description,
            subject = request.subject,
            teacherId = request.teacherId,
            classId = request.classId,
            dueDate = request.dueDate,
            maxScore = request.maxScore,
            createdBy = request.createdBy
        )

        return homeworkService.createHomework(homework)
    }
    
    /**
     * 获取作业详情
     * GET /api/homework/{id}
     */
    @GetMapping("/{id}")
    fun getHomework(@PathVariable id: Long): HomeworkEntity {
        return homeworkService.getHomeworkById(id)
            ?: throw ResourceNotFoundException("作业不存在，ID: $id")
    }
    
    /**
     * 获取作业列表（分页）
     * GET /api/homework/list?pageNum=1&pageSize=10&sortBy=createdAt&sortDir=desc
     * 返回格式：{total, rows, code, msg}
     */
    @GetMapping("/list")
    fun getHomeworkList(
        @RequestParam(defaultValue = "1") pageNum: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): Map<String, Any> {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        // 注意：PageRequest的页码从0开始，而前端传递的pageNum从1开始
        val pageable = PageRequest.of(pageNum - 1, pageSize, sort)
        val homeworkPage = homeworkService.getAllHomework(pageable)

        return mapOf(
            "total" to homeworkPage.totalElements,
            "rows" to homeworkPage.content,
            "code" to 200,
            "msg" to "获取作业列表成功"
        )
    }
    
    /**
     * 根据状态获取作业
     * GET /api/homework/status/{status}
     */
    @GetMapping("/status/{status}")
    fun getHomeworkByStatus(@PathVariable status: HomeworkStatus): List<HomeworkEntity> {
        return homeworkService.getHomeworkByStatus(status)
    }
    
    /**
     * 根据教师ID获取作业
     * GET /api/homework/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    fun getHomeworkByTeacher(@PathVariable teacherId: Long): List<HomeworkEntity> {
        return homeworkService.getHomeworkByTeacher(teacherId)
    }
    
    /**
     * 搜索作业
     * GET /api/homework/search?title=数学
     */
    @GetMapping("/search")
    fun searchHomework(@RequestParam title: String): List<HomeworkEntity> {
        return homeworkService.searchHomeworkByTitle(title)
    }
    
    /**
     * 获取即将到期的作业
     * GET /api/homework/due-soon
     */
    @GetMapping("/due-soon")
    fun getDueSoonHomework(): List<HomeworkEntity> {
        return homeworkService.getDueSoonHomework()
    }
    
    /**
     * 发布作业
     * PUT /api/homework/{id}/publish
     */
    @PutMapping("/{id}/publish")
    fun publishHomework(@PathVariable id: Long): HomeworkEntity {
        return homeworkService.publishHomework(id)
            ?: throw ResourceNotFoundException("作业不存在或发布失败，ID: $id")
    }
    
    /**
     * 获取作业统计信息
     * GET /api/homework/statistics
     */
    @GetMapping("/statistics")
    fun getHomeworkStatistics(): Map<String, Any> {
        val statistics = homeworkService.getHomeworkStatistics()
        // 将HomeworkStatus枚举转换为字符串，便于前端处理
        return statistics.mapKeys { it.key.name }
    }
    
    /**
     * 删除作业
     * DELETE /api/homework/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteHomework(@PathVariable id: Long): Map<String, Any> {
        val success = homeworkService.deleteHomework(id)
        return mapOf(
            "success" to success,
            "message" to if (success) "作业删除成功" else "作业删除失败"
        )
    }
}

/**
 * 创建作业请求DTO
 */
data class CreateHomeworkRequest(
    val title: String,
    val description: String? = null,
    val subject: String? = null,
    val teacherId: Long? = null,
    val classId: Long? = null,
    val dueDate: LocalDateTime? = null,
    val maxScore: Int? = 100,
    val createdBy: String? = null
)
