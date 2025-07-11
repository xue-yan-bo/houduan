package com.jlm.homework.controller

import com.jlm.homework.dto.*
import com.jlm.homework.entity.*
import com.jlm.homework.exception.ParameterException
import com.jlm.homework.exception.ResourceNotFoundException
import com.jlm.homework.service.ExerciseBookServer
import com.jlm.homework.service.UserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 练习册控制器
 * 提供练习册相关的REST接口
 */
@Tag(name = "练习册管理", description = "练习册的创建、查询、更新、删除等操作，支持图片上传")
@RestController
@RequestMapping("/api/exercise-book")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "adminToken")
class ExerciseBookController(
    private val exerciseBookService: ExerciseBookServer,
    private val userService: UserService
) {

    /**
     * 练习册列表查询接口（动态多条件，分页、排序）
     * POST /api/exercise-book/list
     * 前端只需请求此接口即可，支持所有查询条件
     * 
     * 支持的查询条件：
     * - title: 标题模糊查询
     * - subject/subjectId: 学科查询
     * - grade/gradeId: 年级查询
     * - classId/classIds: 班级查询（支持单个或多个）
     * - difficultyLevel: 难度等级查询
     * - creatorId: 创建者查询
     * - status: 状态查询
     * - createdStartTime: 创建时间开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * - createdEndTime: 创建时间结束时间（格式：yyyy-MM-dd HH:mm:ss）
     * 
     * 分页和排序：
     * - pageNum: 页码（从1开始，默认1）
     * - pageSize: 每页大小（默认10，最大100）
     * - sortBy: 排序字段（默认createdAt）
     * - sortDir: 排序方向（asc/desc，默认desc）
     */
    @PostMapping("/list")
    fun list(@RequestBody request: ExerciseBookRequest): Map<String, Any> {
        val validationError = request.validateForQuery()
        if (validationError != null) {
            throw ParameterException(validationError)
        }
        val page = exerciseBookService.searchExerciseBooks(request)
        return mapOf(
            "total" to page.totalElements,
            "rows" to ExerciseBookResponse.fromList(page.content)
        )
    }

    /**
     * 根据ID获取练习册详情
     * GET /api/exercise-book/{id}
     */
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ExerciseBookResponse {
        val entity = exerciseBookService.findById(id)
            ?: throw ResourceNotFoundException("练习册不存在，ID: $id")
        return ExerciseBookResponse.from(entity)
    }

    /**
     * 创建练习册
     * POST /api/exercise-book
     * 支持多班级关联，可以通过classIds字段传递多个班级ID
     */
    @PostMapping
    fun create(@RequestBody request: ExerciseBookRequest): ExerciseBookResponse {
        // 参数验证
        val validationError = request.validateForCreate()
        if (validationError != null) {
            throw ParameterException(validationError)
        }

        // 安全获取当前登录用户ID（如果获取失败会使用默认用户ID）
        val currentUserId = userService.getCurrentUserIdSafely()

        // 转换为实体并保存（toEntity方法已经处理了多班级ID的JSON存储）
        val exerciseBook = request.toEntity(currentUserId)
        val savedEntity = exerciseBookService.save(exerciseBook)
        return ExerciseBookResponse.from(savedEntity)
    }

    /**
     * 更新练习册
     * PUT /api/exercise-book/{id}
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: ExerciseBookRequest
    ): ExerciseBookResponse {
        // 检查练习册是否存在
        val existing = exerciseBookService.findById(id)
            ?: throw ResourceNotFoundException("练习册不存在，ID: $id")

        // 参数验证
        val validationError = request.validateForUpdate()
        if (validationError != null) {
            throw ParameterException(validationError)
        }

        // 更新字段（只更新非空字段）
        var updated = existing.copy(
            title = request.title ?: existing.title,
            description = request.description ?: existing.description,
            subject = request.subject ?: existing.subject,
            subjectId = request.subjectId ?: existing.subjectId,
            grade = request.grade ?: existing.grade,
            gradeId = request.gradeId ?: existing.gradeId,
            classId = request.classId ?: existing.classId,
            difficultyLevel = request.difficultyLevel ?: existing.difficultyLevel,
            status = request.status ?: existing.status
        )

        // 处理图片更新
        if (request.images != null || request.imageUrls.isNotEmpty()) {
            val imageList = when {
                !request.images.isNullOrEmpty() -> request.images!!
                request.imageUrls.isNotEmpty() -> com.jlm.homework.entity.createImagesFromUrls(request.imageUrls)
                else -> emptyList()
            }
            updated = updated.withImages(imageList)
        }

        // 处理班级ID和名称列表的更新
        if (request.classIds.isNotEmpty()) {
            updated = if (request.classNames.isNotEmpty() && request.classNames.size == request.classIds.size) {
                // 如果班级名称列表存在且与ID列表长度一致，同时更新ID和名称
                updated.withClassIdsAndNames(request.classIds, request.classNames)
            } else {
                // 否则只更新ID列表
                updated.withClassIds(request.classIds)
            }
        }

        val savedEntity = exerciseBookService.save(updated)
        return ExerciseBookResponse.from(savedEntity)
    }

    /**
     * 删除练习册
     * DELETE /api/exercise-book/{id}
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): String {
        // 检查练习册是否存在
        exerciseBookService.findById(id)
            ?: throw ResourceNotFoundException("练习册不存在，ID: $id")
        
        exerciseBookService.deleteById(id)
        return "练习册删除成功"
    }

    /**
     * 获取练习册统计信息
     * GET /api/exercise-book/statistics
     */
    @GetMapping("/statistics")
    fun getStatistics(): Map<String, Any> {
        val total = exerciseBookService.count()
        
        return mapOf(
            "total" to total,
            "message" to "统计信息获取成功"
        )
    }
}