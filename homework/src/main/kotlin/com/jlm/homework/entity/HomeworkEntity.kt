package com.jlm.homework.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * 作业实体类
 * 演示Spring Data JPA的使用
 */
@Entity
@Table(name = "homework")
data class HomeworkEntity(
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    
    @Column(name = "title", nullable = false, length = 200)
    val title: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "subject", length = 50)
    val subject: String? = null,
    
    @Column(name = "teacher_id")
    val teacherId: Long? = null,
    
    @Column(name = "class_id")
    val classId: Long? = null,
    
    @Column(name = "due_date")
    val dueDate: LocalDateTime? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    val status: HomeworkStatus = HomeworkStatus.DRAFT,
    
    @Column(name = "max_score")
    val maxScore: Int? = 100,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,
    
    @Column(name = "created_by", length = 50)
    val createdBy: String? = null,
    
    @Column(name = "updated_by", length = 50)
    val updatedBy: String? = null
)

/**
 * 作业状态枚举
 */
enum class HomeworkStatus {
    DRAFT,      // 草稿
    PUBLISHED,  // 已发布
    COMPLETED,  // 已完成
    EXPIRED     // 已过期
}
