package com.jlm.homework.dto

import com.jlm.homework.entity.*

/**
 * 练习册请求转换器
 * 负责将请求DTO转换为实体对象
 */
object ExerciseBookRequestConverter {

    /**
     * 转换为实体对象（用于创建）
     */
    fun toEntity(request: ExerciseBookRequest, currentUserId: Long): ExerciseBookEntity {
        val entity = ExerciseBookEntity(
            title = request.title,
            description = request.description,
            subject = request.subject,
            subjectId = request.subjectId,
            grade = request.grade,
            gradeId = request.gradeId,
            classId = request.classId, // 保留向后兼容
            difficultyLevel = request.difficultyLevel ?: 1,
            creatorId = currentUserId,
            status = request.status ?: ExerciseBookStatus.ACTIVE
        )

        // 处理图片信息
        val imageList = when {
            // 优先使用详细的图片信息
            !request.images.isNullOrEmpty() -> request.images
            // 其次使用URL列表（智能解析）
            request.imageUrls.isNotEmpty() -> createImagesFromUrls(request.imageUrls)
            // 默认为空列表
            else -> emptyList()
        }

        // 先添加图片信息
        var result = entity.withImages(imageList)

        // 处理班级ID和名称列表
        if (request.classIds.isNotEmpty()) {
            result = if (request.classNames.isNotEmpty() && request.classNames.size == request.classIds.size) {
                // 如果班级名称列表存在且与ID列表长度一致，同时设置ID和名称
                result.withClassIdsAndNames(request.classIds, request.classNames)
            } else {
                // 否则只设置ID列表
                result.withClassIds(request.classIds)
            }
        }

        return result
    }
} 