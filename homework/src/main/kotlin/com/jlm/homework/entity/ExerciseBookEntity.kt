package com.jlm.homework.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * 练习册实体类
 * 对应数据库表 exercise_book
 */
@Entity
@Table(name = "exercise_book")
data class ExerciseBookEntity(
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    /**
     * 练习册标题
     */
    @Column(name = "title", nullable = false, length = 200)
    val title: String? = null,

    /**
     * 练习册描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * 学科名称（保留用于显示）
     */
    @Column(name = "subject", length = 50)
    val subject: String? = null,

    /**
     * 学科ID
     */
    @Column(name = "subject_id")
    val subjectId: Long? = null,

    /**
     * 年级名称（保留用于显示）
     */
    @Column(name = "grade", length = 20)
    val grade: String? = null,

    /**
     * 年级ID
     */
    @Column(name = "grade_id")
    val gradeId: Long? = null,

    /**
     * 班级ID
     */
    @Column(name = "class_id")
    val classId: Long? = null,

    /**
     * 难度等级 (1-5)
     */
    @Column(name = "difficulty_level")
    val difficultyLevel: Int? = 1,

    /**
     * 创建者ID
     */
    @Column(name = "creator_id")
    val creatorId: Long? = null,

    /**
     * 状态 (ACTIVE, INACTIVE, DELETED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    val status: ExerciseBookStatus = ExerciseBookStatus.ACTIVE,

    /**
     * 练习册图片列表（JSON格式存储）
     */
    @Column(name = "images", columnDefinition = "JSON")
    val images: String? = null,

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonIgnore
    val createdAt: LocalDateTime? = null,

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    val updatedAt: LocalDateTime? = null
)

/**
 * 练习册状态枚举
 */
enum class ExerciseBookStatus {
    /**
     * 活跃状态
     */
    ACTIVE,

    /**
     * 非活跃状态
     */
    INACTIVE,

    /**
     * 已删除状态
     */
    DELETED
}

/**
 * 练习册图片信息
 */
data class ExerciseBookImage(
    /**
     * 图片URL
     */
    @JsonProperty("url")
    val url: String,

    /**
     * 图片描述
     */
    @JsonProperty("description")
    val description: String? = null,

    /**
     * 图片类型（封面图、内容图等）
     */
    @JsonProperty("type")
    val type: String? = "content",

    /**
     * 排序顺序
     */
    @JsonProperty("order")
    val order: Int? = 0
) {
    // 无参构造函数，用于Jackson反序列化
    constructor() : this("", null, "content", 0)
}

/**
 * ExerciseBookEntity 扩展属性和方法
 */

// 单例ObjectMapper，避免重复创建
private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

/**
 * 获取图片列表
 */
val ExerciseBookEntity.imageList: List<ExerciseBookImage>
    get() = try {
        if (images.isNullOrBlank()) {
            emptyList()
        } else {
            objectMapper.readValue(
                images,
                object : com.fasterxml.jackson.core.type.TypeReference<List<ExerciseBookImage>>() {}
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

/**
 * 获取封面图片URL
 */
val ExerciseBookEntity.coverImageUrl: String?
    get() = imageList.firstOrNull { it.type == "cover" }?.url
        ?: imageList.firstOrNull()?.url

/**
 * 获取所有图片URL列表
 */
val ExerciseBookEntity.imageUrls: List<String>
    get() = imageList.map { it.url }

/**
 * 创建带图片的练习册实体
 */
fun ExerciseBookEntity.withImages(imageList: List<ExerciseBookImage>): ExerciseBookEntity {
    val imagesJson = if (imageList.isEmpty()) {
        null
    } else {
        try {
            objectMapper.writeValueAsString(imageList)
        } catch (e: Exception) {
            null
        }
    }

    return this.copy(images = imagesJson)
}

/**
 * 从URL列表创建图片信息
 */
fun createImagesFromUrls(urls: List<String>): List<ExerciseBookImage> {
    return urls.mapIndexed { index, url ->
        ExerciseBookImage(
            url = url,
            type = if (index == 0) "cover" else "content",
            order = index
        )
    }
}