package com.jlm.homework.controller

import com.jlm.homework.entity.HomeworkEntity
import com.jlm.homework.entity.HomeworkStatus
import com.jlm.homework.service.HomeworkService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag

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
    fun createHomework(@RequestBody request: CreateHomeworkRequest): ResponseEntity<Any> {
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
        
        val savedHomework = homeworkService.createHomework(homework)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to savedHomework,
            "message" to "作业创建成功"
        ))
    }
    
    /**
     * 获取作业详情
     * GET /api/homework/{id}
     */
    @GetMapping("/{id}")
    fun getHomework(@PathVariable id: Long): ResponseEntity<Any> {
        val homework = homeworkService.getHomeworkById(id)
        
        return if (homework != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to homework,
                "message" to "获取作业成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "作业不存在"
            ))
        }
    }
    
    /**
     * 获取作业列表（分页）
     * GET /api/homework?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    fun getHomeworkList(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Any> {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        val homeworkPage = homeworkService.getAllHomework(pageable)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "content" to homeworkPage.content,
                "totalElements" to homeworkPage.totalElements,
                "totalPages" to homeworkPage.totalPages,
                "currentPage" to homeworkPage.number,
                "size" to homeworkPage.size
            ),
            "message" to "获取作业列表成功"
        ))
    }
    
    /**
     * 根据状态获取作业
     * GET /api/homework/status/{status}
     */
    @GetMapping("/status/{status}")
    fun getHomeworkByStatus(@PathVariable status: HomeworkStatus): ResponseEntity<Any> {
        val homeworkList = homeworkService.getHomeworkByStatus(status)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to homeworkList,
            "message" to "获取${status}状态作业成功"
        ))
    }
    
    /**
     * 根据教师ID获取作业
     * GET /api/homework/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    fun getHomeworkByTeacher(@PathVariable teacherId: Long): ResponseEntity<Any> {
        val homeworkList = homeworkService.getHomeworkByTeacher(teacherId)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to homeworkList,
            "message" to "获取教师作业成功"
        ))
    }
    
    /**
     * 搜索作业
     * GET /api/homework/search?title=数学
     */
    @GetMapping("/search")
    fun searchHomework(@RequestParam title: String): ResponseEntity<Any> {
        val homeworkList = homeworkService.searchHomeworkByTitle(title)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to homeworkList,
            "message" to "搜索作业成功"
        ))
    }
    
    /**
     * 获取即将到期的作业
     * GET /api/homework/due-soon
     */
    @GetMapping("/due-soon")
    fun getDueSoonHomework(): ResponseEntity<Any> {
        val homeworkList = homeworkService.getDueSoonHomework()
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to homeworkList,
            "message" to "获取即将到期作业成功"
        ))
    }
    
    /**
     * 发布作业
     * PUT /api/homework/{id}/publish
     */
    @PutMapping("/{id}/publish")
    fun publishHomework(@PathVariable id: Long): ResponseEntity<Any> {
        val homework = homeworkService.publishHomework(id)
        
        return if (homework != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to homework,
                "message" to "作业发布成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "作业不存在或发布失败"
            ))
        }
    }
    
    /**
     * 获取作业统计信息
     * GET /api/homework/statistics
     */
    @GetMapping("/statistics")
    fun getHomeworkStatistics(): ResponseEntity<Any> {
        val statistics = homeworkService.getHomeworkStatistics()
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to statistics,
            "message" to "获取作业统计成功"
        ))
    }
    
    /**
     * 删除作业
     * DELETE /api/homework/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteHomework(@PathVariable id: Long): ResponseEntity<Any> {
        val success = homeworkService.deleteHomework(id)
        
        return ResponseEntity.ok(mapOf(
            "success" to success,
            "data" to null,
            "message" to if (success) "作业删除成功" else "作业删除失败"
        ))
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
