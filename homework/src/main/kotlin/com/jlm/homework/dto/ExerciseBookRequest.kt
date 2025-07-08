package com.jlm.homework.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jlm.homework.entity.*

/**
 * 练习册请求DTO
 * 统一用于创建、更新和查询练习册的请求数据
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExerciseBookRequest(
    /**
     * 练习册标题
     */
    val title: String? = null,

    /**
     * 练习册描述
     */
    val description: String? = null,

    /**
     * 学科名称（用于显示）
     */
    val subject: String? = null,

    /**
     * 学科ID
     */
    val subjectId: Long? = null,

    /**
     * 年级名称（用于显示）
     */
    val grade: String? = null,

    /**
     * 年级ID
     */
    val gradeId: Long? = null,

    /**
     * 班级ID（单个，保留向后兼容）
     */
    val classId: Long? = null,

    /**
     * 班级ID列表（多选支持）
     * 支持数组格式：[1, 2, 3] 或逗号分隔字符串格式："1,2,3"
     */
    @JsonProperty("classIds")
    private val _classIds: Any? = null,

    /**
     * 班级名称列表（多选支持）
     * 支持数组格式：["一班", "二班"] 或逗号分隔字符串格式："一班,二班"
     */
    @JsonProperty("classNames")
    private val _classNames: Any? = null,

    /**
     * 难度等级 (1-5)
     */
    val difficultyLevel: Int? = null,

    /**
     * 创建者ID
     */
    val creatorId: Long? = null,

    /**
     * 状态 (ACTIVE, INACTIVE, DELETED)
     */
    val status: ExerciseBookStatus? = null,

    /**
     * 图片URL列表（简化版，用于接收前端数据）
     * 支持数组格式：["url1", "url2"] 或逗号分隔字符串格式："url1,url2"
     */
    @JsonProperty("imageUrls")
    private val _imageUrls: Any? = null,

    /**
     * 图片信息列表（详细版，包含描述、类型等）
     */
    val images: List<ExerciseBookImage>? = null,

    // 查询相关字段
    /**
     * 页码（从1开始）
     */
    val pageNum: Int = 1,

    /**
     * 每页大小
     */
    val pageSize: Int = 10,

    /**
     * 排序字段
     */
    val sortBy: String = "createdAt",

    /**
     * 排序方向 (asc/desc)
     */
    val sortDir: String = "desc"
) {
    /**
     * 获取解析后的图片URL列表
     * 智能处理多种输入格式，兼容前端传递字符串或数组
     */
    val imageUrls: List<String>
        get() = when (_imageUrls) {
            is List<*> -> _imageUrls.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            is String -> _imageUrls.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }

    /**
     * 获取解析后的班级ID列表
     * 智能处理多种输入格式，兼容前端传递数组或逗号分隔字符串
     */
    val classIds: List<Long>
        get() = when (_classIds) {
            is List<*> -> _classIds.mapNotNull {
                when (it) {
                    is Number -> it.toLong()
                    is String -> it.trim().toLongOrNull()
                    else -> null
                }
            }
            is String -> _classIds.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { it.toLongOrNull() }
            else -> classId?.let { listOf(it) } ?: emptyList()
        }

    /**
     * 获取解析后的班级名称列表
     * 智能处理多种输入格式，兼容前端传递数组或逗号分隔字符串
     */
    val classNames: List<String>
        get() = when (_classNames) {
            is List<*> -> _classNames.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            is String -> _classNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }

    /**
     * 验证创建请求参数
     */
    fun validateForCreate(): String? {
        if (title.isNullOrBlank()) {
            return "练习册标题不能为空"
        }
        if (title.length > 200) {
            return "练习册标题长度不能超过200个字符"
        }
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        return null
    }

    /**
     * 验证更新请求参数
     */
    fun validateForUpdate(): String? {
        if (title != null) {
            if (title.isBlank()) {
                return "练习册标题不能为空"
            }
            if (title.length > 200) {
                return "练习册标题长度不能超过200个字符"
            }
        }
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        return null
    }

    /**
     * 验证查询请求参数
     */
    fun validateForQuery(): String? {
        if (pageNum < 1) {
            return "页码必须大于0"
        }
        if (pageSize <= 0 || pageSize > 100) {
            return "每页大小必须在1-100之间"
        }
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        if (sortDir.lowercase() !in listOf("asc", "desc")) {
            return "排序方向只能是asc或desc"
        }
        return null
    }

    /**
     * 转换为实体对象（用于创建）
     */
    fun toEntity(currentUserId: Long): ExerciseBookEntity {
        val entity = ExerciseBookEntity(
            title = title,
            description = description,
            subject = subject,
            subjectId = subjectId,
            grade = grade,
            gradeId = gradeId,
            classId = classId, // 保留向后兼容
            difficultyLevel = difficultyLevel ?: 1,
            creatorId = currentUserId,
            status = status ?: ExerciseBookStatus.ACTIVE
        )

        // 处理图片信息
        val imageList = when {
            // 优先使用详细的图片信息
            !images.isNullOrEmpty() -> images
            // 其次使用URL列表（智能解析）
            imageUrls.isNotEmpty() -> createImagesFromUrls(imageUrls)
            // 默认为空列表
            else -> emptyList()
        }

        // 先添加图片信息
        var result = entity.withImages(imageList)

        // 处理班级ID和名称列表
        if (classIds.isNotEmpty()) {
            result = if (classNames.isNotEmpty() && classNames.size == classIds.size) {
                // 如果班级名称列表存在且与ID列表长度一致，同时设置ID和名称
                result.withClassIdsAndNames(classIds, classNames)
            } else {
                // 否则只设置ID列表
                result.withClassIds(classIds)
            }
        }

        return result
    }

    /**
     * 检查是否有非空的查询条件
     */
    fun hasSearchConditions(): Boolean {
        return !title.isNullOrBlank() ||
               !subject.isNullOrBlank() ||
               subjectId != null ||
               !grade.isNullOrBlank() ||
               gradeId != null ||
               classId != null ||
               difficultyLevel != null ||
               creatorId != null
    }
}
