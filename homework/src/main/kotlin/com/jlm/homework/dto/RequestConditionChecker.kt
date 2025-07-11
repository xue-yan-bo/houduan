package com.jlm.homework.dto

/**
 * 请求条件检查器
 * 负责检查请求中是否包含有效的查询条件
 */
object RequestConditionChecker {

    /**
     * 检查是否有非空的查询条件
     */
    fun hasSearchConditions(request: ExerciseBookRequest): Boolean {
        return !request.title.isNullOrBlank() ||
               !request.subject.isNullOrBlank() ||
               request.subjectId != null ||
               !request.grade.isNullOrBlank() ||
               request.gradeId != null ||
               request.classId != null ||
               request.difficultyLevel != null ||
               request.creatorId != null ||
               request.parsedCreatedStartTime != null ||
               request.parsedCreatedEndTime != null
    }
} 