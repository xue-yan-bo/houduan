package com.jlm.homework.controller

import com.jlm.homework.dto.ExerciseBookRequest
import com.jlm.homework.dto.ExerciseBookResponse
import com.jlm.homework.entity.ExerciseBookStatus
import com.jlm.homework.entity.withImages
import com.jlm.homework.exception.ParameterException
import com.jlm.homework.exception.ResourceNotFoundException
import com.jlm.homework.service.ExerciseBookServer
import com.jlm.homework.service.UserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
     * 获取练习册列表（分页）
     * GET /api/exercise-book/list?page=0&size=10&sort=createdAt,desc
     * 返回格式：{total, rows, code, msg}
     */
    @GetMapping("/list")
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): Map<String, Any> {
        // 参数验证
        if (page < 0) {
            throw ParameterException("页码不能小于0")
        }
        if (size <= 0 || size > 100) {
            throw ParameterException("每页大小必须在1-100之间")
        }

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val pageResult = exerciseBookService.findAllWithPage(pageable)

        return mapOf(
            "total" to pageResult.totalElements,
            "rows" to ExerciseBookResponse.fromList(pageResult.content)
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
        if (request.images != null || request.imageUrls != null) {
            val imageList = when {
                !request.images.isNullOrEmpty() -> request.images
                !request.imageUrls.isNullOrEmpty() -> com.jlm.homework.entity.createImagesFromUrls(request.imageUrls)
                else -> emptyList()
            }
            updated = updated.withImages(imageList)
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
     * 根据标题搜索练习册
     * GET /api/exercise-book/search?title=数学
     */
    @GetMapping("/search")
    fun searchByTitle(@RequestParam title: String): List<ExerciseBookResponse> {
        if (title.isBlank()) {
            throw ParameterException("搜索标题不能为空")
        }

        val entities = exerciseBookService.findByTitleContaining(title)
        return ExerciseBookResponse.fromList(entities)
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

    /**
     * 动态查询练习册
     * POST /api/exercise-book/search
     * 支持多条件动态查询，返回统一格式：{total, rows, code, msg}
     */
    @PostMapping("/search")
    fun searchExerciseBooks(@RequestBody request: ExerciseBookRequest): Map<String, Any> {
        // 参数验证
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
     * 简化的动态查询接口（GET方式）
     * GET /api/exercise-book/query?title=xxx&subject=xxx&grade=xxx&pageNum=1&pageSize=10
     */
    @GetMapping("/query")
    fun queryExerciseBooks(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) subject: String?,
        @RequestParam(required = false) subjectId: Long?,
        @RequestParam(required = false) grade: String?,
        @RequestParam(required = false) gradeId: Long?,
        @RequestParam(required = false) classId: Long?,
        @RequestParam(required = false) difficultyLevel: Int?,
        @RequestParam(required = false) creatorId: Long?,
        @RequestParam(required = false) status: ExerciseBookStatus?,
        @RequestParam(defaultValue = "1") pageNum: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): Map<String, Any> {

        val request = ExerciseBookRequest(
            title = title,
            subject = subject,
            subjectId = subjectId,
            grade = grade,
            gradeId = gradeId,
            classId = classId,
            difficultyLevel = difficultyLevel,
            creatorId = creatorId,
            status = status ?: ExerciseBookStatus.ACTIVE,
            pageNum = pageNum,
            pageSize = pageSize,
            sortBy = sortBy,
            sortDir = sortDir
        )

        return searchExerciseBooks(request)
    }
}