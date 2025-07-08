package com.jlm.homework.repository

import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.entity.ExerciseBookStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 练习册数据访问层
 * 提供练习册相关的数据库操作
 */
interface ExerciseBookRepo : JpaRepository<ExerciseBookEntity, Long> {

    /**
     * 根据标题模糊查询练习册
     * @param title 标题关键词
     * @return 匹配的练习册列表
     */
    fun findByTitleContainingIgnoreCase(title: String): List<ExerciseBookEntity>

    /**
     * 根据状态查询练习册
     * @param status 练习册状态
     * @return 指定状态的练习册列表
     */
    fun findByStatus(status: ExerciseBookStatus): List<ExerciseBookEntity>

    /**
     * 根据学科查询练习册
     * @param subject 学科名称
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findBySubject(subject: String, pageable: Pageable): Page<ExerciseBookEntity>

    /**
     * 根据年级查询练习册
     * @param grade 年级
     * @return 对应年级的练习册列表
     */
    fun findByGrade(grade: String): List<ExerciseBookEntity>

    /**
     * 根据创建者ID查询练习册
     * @param creatorId 创建者ID
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findByCreatorId(creatorId: Long, pageable: Pageable): Page<ExerciseBookEntity>

    /**
     * 根据难度等级查询练习册
     * @param difficultyLevel 难度等级
     * @return 对应难度的练习册列表
     */
    fun findByDifficultyLevel(difficultyLevel: Int): List<ExerciseBookEntity>

    /**
     * 统计指定状态的练习册数量
     * @param status 练习册状态
     * @return 数量
     */
    fun countByStatus(status: ExerciseBookStatus): Long

    /**
     * 自定义查询：根据多个条件查询练习册
     * @param title 标题关键词（可选）
     * @param subject 学科（可选）
     * @param grade 年级（可选）
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    @Query("""
        SELECT e FROM ExerciseBookEntity e
        WHERE (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:subject IS NULL OR e.subject = :subject)
        AND (:grade IS NULL OR e.grade = :grade)
        AND e.status = :status
        ORDER BY e.createdAt DESC
    """)
    fun findByConditions(
        @Param("title") title: String?,
        @Param("subject") subject: String?,
        @Param("grade") grade: String?,
        @Param("status") status: ExerciseBookStatus,
        pageable: Pageable
    ): Page<ExerciseBookEntity>

    /**
     * 增强的动态查询：支持更多查询条件
     * @param title 标题关键词（可选）
     * @param subject 学科名称（可选）
     * @param subjectId 学科ID（可选）
     * @param grade 年级名称（可选）
     * @param gradeId 年级ID（可选）
     * @param classId 班级ID（可选）
     * @param difficultyLevel 难度等级（可选）
     * @param creatorId 创建者ID（可选）
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    @Query("""
        SELECT e FROM ExerciseBookEntity e
        WHERE (:title IS NULL OR TRIM(:title) = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:subject IS NULL OR TRIM(:subject) = '' OR e.subject = :subject)
        AND (:subjectId IS NULL OR e.subjectId = :subjectId)
        AND (:grade IS NULL OR TRIM(:grade) = '' OR e.grade = :grade)
        AND (:gradeId IS NULL OR e.gradeId = :gradeId)
        AND (:classId IS NULL OR e.classId = :classId)
        AND (:difficultyLevel IS NULL OR e.difficultyLevel = :difficultyLevel)
        AND (:creatorId IS NULL OR e.creatorId = :creatorId)
        AND (:status IS NULL OR e.status = :status)
        ORDER BY e.createdAt DESC
    """)
    fun findByDynamicConditions(
        @Param("title") title: String?,
        @Param("subject") subject: String?,
        @Param("subjectId") subjectId: Long?,
        @Param("grade") grade: String?,
        @Param("gradeId") gradeId: Long?,
        @Param("classId") classId: Long?,
        @Param("difficultyLevel") difficultyLevel: Int?,
        @Param("creatorId") creatorId: Long?,
        @Param("status") status: ExerciseBookStatus?,
        pageable: Pageable
    ): Page<ExerciseBookEntity>
}