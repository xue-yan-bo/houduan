package com.jlm.homework.entity

import jakarta.persistence.*
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
     * 学科
     */
    @Column(name = "subject", length = 50)
    val subject: String? = null,
    
    /**
     * 年级
     */
    @Column(name = "grade", length = 20)
    val grade: String? = null,
    
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
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
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