package com.jlm.homework.controller

import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.exception.ParameterException
import com.jlm.homework.exception.ResourceNotFoundException
import com.jlm.homework.service.ExerciseBookServer
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * 练习册控制器
 * 提供练习册相关的REST接口
 */
@RestController
@RequestMapping("/api/exercise-book")
class ExerciseBookController(
    private val exerciseBookService: ExerciseBookServer
) {

    /**
     * 获取练习册列表（分页）
     * GET /api/exercise-book/list?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/list")
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): List<ExerciseBookEntity> {
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
        return exerciseBookService.findAll(pageable)
    }

    /**
     * 根据ID获取练习册详情
     * GET /api/exercise-book/{id}
     */
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ExerciseBookEntity {
        return exerciseBookService.findById(id)
            ?: throw ResourceNotFoundException("练习册不存在，ID: $id")
    }

    /**
     * 创建练习册
     * POST /api/exercise-book
     */
    @PostMapping
    fun create(@RequestBody exerciseBook: ExerciseBookEntity): ExerciseBookEntity {
        // 基本参数验证
        if (exerciseBook.title.isNullOrBlank()) {
            throw ParameterException("练习册标题不能为空")
        }
        
        return exerciseBookService.save(exerciseBook)
    }

    /**
     * 更新练习册
     * PUT /api/exercise-book/{id}
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody exerciseBook: ExerciseBookEntity
    ): ExerciseBookEntity {
        // 检查练习册是否存在
        val existing = exerciseBookService.findById(id)
            ?: throw ResourceNotFoundException("练习册不存在，ID: $id")
        
        // 基本参数验证
        if (exerciseBook.title.isNullOrBlank()) {
            throw ParameterException("练习册标题不能为空")
        }
        
        // 更新字段
        val updated = existing.copy(
            title = exerciseBook.title,
            description = exerciseBook.description,
            updatedAt = java.time.LocalDateTime.now()
        )
        
        return exerciseBookService.save(updated)
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
    fun searchByTitle(@RequestParam title: String): List<ExerciseBookEntity> {
        if (title.isBlank()) {
            throw ParameterException("搜索标题不能为空")
        }
        
        return exerciseBookService.findByTitleContaining(title)
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