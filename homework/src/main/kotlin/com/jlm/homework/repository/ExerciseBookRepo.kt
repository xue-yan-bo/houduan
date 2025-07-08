package com.jlm.homework.repository

import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.entity.ExerciseBookStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

/**
 * 练习册数据访问层
 * 继承JpaSpecificationExecutor支持动态条件查询
 */
interface ExerciseBookRepo : JpaRepository<ExerciseBookEntity, Long>, JpaSpecificationExecutor<ExerciseBookEntity> {

    /**
     * 根据标题模糊查询练习册
     */
    fun findByTitleContainingIgnoreCase(title: String): List<ExerciseBookEntity>

    /**
     * 根据状态查询练习册
     */
    fun findByStatus(status: ExerciseBookStatus): List<ExerciseBookEntity>

    /**
     * 根据学科分页查询练习册
     */
    fun findBySubject(subject: String, pageable: Pageable): Page<ExerciseBookEntity>

    /**
     * 根据年级查询练习册
     */
    fun findByGrade(grade: String): List<ExerciseBookEntity>

    /**
     * 根据创建者ID分页查询练习册
     */
    fun findByCreatorId(creatorId: Long, pageable: Pageable): Page<ExerciseBookEntity>

    /**
     * 根据难度等级查询练习册
     */
    fun findByDifficultyLevel(difficultyLevel: Int): List<ExerciseBookEntity>

    /**
     * 统计指定状态的练习册数量
     */
    fun countByStatus(status: ExerciseBookStatus): Long
}