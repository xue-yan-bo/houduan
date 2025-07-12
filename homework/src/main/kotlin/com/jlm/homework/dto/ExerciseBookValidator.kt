package com.jlm.homework.dto

/**
 * 练习册验证器
 * 负责验证练习册请求参数的合法性
 */
object ExerciseBookValidator {

    /**
     * 验证创建请求参数
     */
    fun validateForCreate(request: ExerciseBookRequest): String? {
        if (request.title.isNullOrBlank()) {
            return "练习册标题不能为空"
        }
        if (request.title.length > 200) {
            return "练习册标题长度不能超过200个字符"
        }
        if (request.difficultyLevel != null && (request.difficultyLevel < 1 || request.difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        return null
    }

    /**
     * 验证更新请求参数
     */
    fun validateForUpdate(request: ExerciseBookRequest): String? {
        if (request.title != null) {
            if (request.title.isBlank()) {
                return "练习册标题不能为空"
            }
            if (request.title.length > 200) {
                return "练习册标题长度不能超过200个字符"
            }
        }
        if (request.difficultyLevel != null && (request.difficultyLevel < 1 || request.difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        return null
    }

    /**
     * 验证查询请求参数
     */
    fun validateForQuery(request: ExerciseBookRequest): String? {
        if (request.pageNum < 1) {
            return "页码必须大于0"
        }
        if (request.pageSize <= 0 || request.pageSize > 100) {
            return "每页大小必须在1-100之间"
        }
        if (request.difficultyLevel != null && (request.difficultyLevel < 1 || request.difficultyLevel > 5)) {
            return "难度等级必须在1-5之间"
        }
        if (request.sortDir.lowercase() !in listOf("asc", "desc")) {
            return "排序方向只能是asc或desc"
        }
        // 验证时间区间：结束时间不能早于开始时间
        val startTime = request.parsedCreatedStartTime
        val endTime = request.parsedCreatedEndTime
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            return "创建时间结束时间不能早于开始时间"
        }
        return null
    }
} 