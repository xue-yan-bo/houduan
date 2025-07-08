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
     * 增强的动态查询：支持更多查询条件（包含多班级JSON字段支持）
     * @param title 标题关键词（可选）
     * @param subject 学科名称（可选）
     * @param subjectId 学科ID（可选）
     * @param grade 年级名称（可选）
     * @param gradeId 年级ID（可选）
     * @param classId 班级ID（可选，向后兼容）
     * @param classIds 班级ID列表（可选，多班级支持）
     * @param difficultyLevel 难度等级（可选）
     * @param creatorId 创建者ID（可选）
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    @Query(value = """
        SELECT * FROM exercise_book e
        WHERE (:title IS NULL OR TRIM(:title) = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:subject IS NULL OR TRIM(:subject) = '' OR e.subject = :subject)
        AND (:subject_id IS NULL OR e.subject_id = :subject_id)
        AND (:grade IS NULL OR TRIM(:grade) = '' OR e.grade = :grade)
        AND (:grade_id IS NULL OR e.grade_id = :grade_id)
        AND (
            (:class_id IS NULL AND (:class_ids IS NULL OR JSON_LENGTH(:class_ids) = 0)) OR
            (:class_id IS NOT NULL AND (e.class_id = :class_id OR JSON_CONTAINS(e.class_ids, CAST(:class_id AS JSON)))) OR
            (:class_ids IS NOT NULL AND JSON_LENGTH(:class_ids) > 0 AND (
                e.class_id IN (SELECT JSON_UNQUOTE(JSON_EXTRACT(:class_ids, CONCAT('$[', numbers.n, ']')))
                               FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) numbers
                               WHERE JSON_UNQUOTE(JSON_EXTRACT(:class_ids, CONCAT('$[', numbers.n, ']'))) IS NOT NULL) OR
                JSON_OVERLAPS(e.class_ids, :class_ids)
            ))
        )
        AND (:difficulty_level IS NULL OR e.difficulty_level = :difficulty_level)
        AND (:creator_id IS NULL OR e.creator_id = :creator_id)
        AND (:status IS NULL OR e.status = :status)
        ORDER BY e.created_at DESC
    """,
    countQuery = """
        SELECT COUNT(*) FROM exercise_book e
        WHERE (:title IS NULL OR TRIM(:title) = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:subject IS NULL OR TRIM(:subject) = '' OR e.subject = :subject)
        AND (:subject_id IS NULL OR e.subject_id = :subject_id)
        AND (:grade IS NULL OR TRIM(:grade) = '' OR e.grade = :grade)
        AND (:grade_id IS NULL OR e.grade_id = :grade_id)
        AND (
            (:class_id IS NULL AND (:class_ids IS NULL OR JSON_LENGTH(:class_ids) = 0)) OR
            (:class_id IS NOT NULL AND (e.class_id = :class_id OR JSON_CONTAINS(e.class_ids, CAST(:class_id AS JSON)))) OR
            (:class_ids IS NOT NULL AND JSON_LENGTH(:class_ids) > 0 AND (
                e.class_id IN (SELECT JSON_UNQUOTE(JSON_EXTRACT(:class_ids, CONCAT('$[', numbers.n, ']')))
                               FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) numbers
                               WHERE JSON_UNQUOTE(JSON_EXTRACT(:class_ids, CONCAT('$[', numbers.n, ']'))) IS NOT NULL) OR
                JSON_OVERLAPS(e.class_ids, :class_ids)
            ))
        )
        AND (:difficulty_level IS NULL OR e.difficulty_level = :difficulty_level)
        AND (:creator_id IS NULL OR e.creator_id = :creator_id)
        AND (:status IS NULL OR e.status = :status)
    """,
    nativeQuery = true)
    fun findByDynamicConditions(
        @Param("title") title: String?,
        @Param("subject") subject: String?,
        @Param("subject_id") subjectId: Long?,
        @Param("grade") grade: String?,
        @Param("grade_id") gradeId: Long?,
        @Param("class_id") classId: Long?,
        @Param("class_ids") classIds: String?,
        @Param("difficulty_level") difficultyLevel: Int?,
        @Param("creator_id") creatorId: Long?,
        @Param("status") status: String?,
        pageable: Pageable
    ): Page<ExerciseBookEntity>
}