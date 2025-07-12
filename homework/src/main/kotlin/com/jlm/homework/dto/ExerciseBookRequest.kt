package com.jlm.homework.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jlm.homework.entity.ExerciseBookImage
import com.jlm.homework.entity.ExerciseBookStatus
import java.time.LocalDateTime

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
    @field:JsonProperty("classIds")
    private val _classIds: Any? = null,

    /**
     * 班级名称列表（多选支持）
     * 支持数组格式：["一班", "二班"] 或逗号分隔字符串格式："一班,二班"
     * 只用于反序列化，避免与业务属性classNames冲突
     */
    @field:JsonProperty("classNames")
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
     * 学校ID（租户标识）
     */
    val schoolId: Long? = null,

    /**
     * 状态 (ACTIVE, INACTIVE, DELETED)
     */
    val status: ExerciseBookStatus? = null,

    /**
     * 图片URL列表（简化版，用于接收前端数据）
     * 支持数组格式：["url1", "url2"] 或逗号分隔字符串格式："url1,url2"
     */
    @field:JsonProperty("imageUrls")
    private val _imageUrls: Any? = null,

    /**
     * 图片信息列表（详细版，包含描述、类型等）
     */
    val images: List<ExerciseBookImage>? = null,

    // 时间查询相关字段
    /**
     * 创建时间查询 - 开始时间
     * 格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    val createdStartTime: LocalDateTime? = null,

    /**
     * 创建时间查询 - 结束时间
     * 格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    val createdEndTime: LocalDateTime? = null,

    /**
     * 创建时间查询 - 数组格式（前端兼容）
     * 格式：["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"]
     * 第一个元素为开始时间，第二个元素为结束时间
     */
    @field:JsonProperty("createdAt")
    private val _createdAt: Any? = null,

    // 查询相关字段
    /**
     * 页码（从1开始）
     */
    val pageNum: Int = 1,

    /**
     * 前端分页参数（从0开始，兼容前端）
     */
    @field:JsonProperty("page")
    private val _page: Int? = null,

    /**
     * 每页大小
     */
    val pageSize: Int = 10,

    /**
     * 前端每页大小参数（兼容前端）
     */
    @field:JsonProperty("size")
    private val _size: Int? = null,

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
     */
    val imageUrls: List<String>
        get() = RequestDataParser.parseStringList(_imageUrls)

    /**
     * 获取解析后的班级ID列表
     */
    val classIds: List<Long>
        get() = RequestDataParser.parseLongList(_classIds, classId)

    /**
     * 获取解析后的班级名称列表
     * 只做业务使用，不参与序列化/反序列化，避免Jackson冲突
     */
    val classNames: List<String>
        get() = RequestDataParser.parseStringList(_classNames)

    /**
     * 获取解析后的创建时间开始时间
     */
    val parsedCreatedStartTime: LocalDateTime?
        get() = RequestTimeParser.parseStartTime(_createdAt, createdStartTime)

    /**
     * 获取解析后的创建时间结束时间
     */
    val parsedCreatedEndTime: LocalDateTime?
        get() = RequestTimeParser.parseEndTime(_createdAt, createdEndTime)

    /**
     * 获取实际页码（自动处理前端0开始的分页）
     */
    val actualPageNum: Int
        get() = _page?.let { it + 1 } ?: pageNum

    /**
     * 获取实际每页大小
     */
    val actualPageSize: Int
        get() = _size ?: pageSize

    /**
     * 检查是否有非空的查询条件
     */
    fun hasSearchConditions(): Boolean {
        return RequestConditionChecker.hasSearchConditions(this)
    }
}
