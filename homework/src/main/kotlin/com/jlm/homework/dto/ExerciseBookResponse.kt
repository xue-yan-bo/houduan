package com.jlm.homework.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.jlm.homework.entity.*
import java.time.LocalDateTime

/**
 * 练习册响应DTO
 * 用于返回给前端的练习册数据，包含班级ID数组和名称数组
 */
data class ExerciseBookResponse(
    /**
     * 主键ID
     */
    val id: Long?,

    /**
     * 练习册标题
     */
    val title: String?,

    /**
     * 练习册描述
     */
    val description: String?,

    /**
     * 学科名称
     */
    val subject: String?,

    /**
     * 学科ID
     */
    val subjectId: Long?,

    /**
     * 年级名称
     */
    val grade: String?,

    /**
     * 年级ID
     */
    val gradeId: Long?,

    /**
     * 班级ID（单个，保留向后兼容）
     */
    val classId: Long?,

    /**
     * 班级ID数组（前端友好格式）
     */
    val classIds: List<Long>,

    /**
     * 班级名称数组（前端友好格式）
     */
    val classNames: List<String>,

    /**
     * 难度等级
     */
    val difficultyLevel: Int?,

    /**
     * 创建者ID
     */
    val creatorId: Long?,

    /**
     * 学校ID（租户标识）
     */
    val schoolId: Long?,

    /**
     * 状态
     */
    val status: ExerciseBookStatus,

    /**
     * 图片URL列表
     */
    val imageUrls: List<String>,

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime?,

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * 从实体类转换为响应DTO
         */
        fun from(entity: ExerciseBookEntity): ExerciseBookResponse {
            return ExerciseBookResponse(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                subject = entity.subject,
                subjectId = entity.subjectId,
                grade = entity.grade,
                gradeId = entity.gradeId,
                classId = entity.classId,
                classIds = entity.classIdList,
                classNames = entity.classNameList,
                difficultyLevel = entity.difficultyLevel,
                creatorId = entity.creatorId,
                schoolId = entity.schoolId,
                status = entity.status,
                imageUrls = entity.imageUrls,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }

        /**
         * 批量转换实体列表为响应DTO列表
         */
        fun fromList(entities: List<ExerciseBookEntity>): List<ExerciseBookResponse> {
            return entities.map { from(it) }
        }
    }
}
