package com.jlm.homework.dto

import com.jlm.homework.entity.ExerciseBookEntity

/**
 * 练习册请求工具类
 * 为ExerciseBookRequest提供扩展方法，统一调用各个工具类的功能
 */

/**
 * 验证创建请求参数
 */
fun ExerciseBookRequest.validateForCreate(): String? {
    return ExerciseBookValidator.validateForCreate(this)
}

/**
 * 验证更新请求参数
 */
fun ExerciseBookRequest.validateForUpdate(): String? {
    return ExerciseBookValidator.validateForUpdate(this)
}

/**
 * 验证查询请求参数
 */
fun ExerciseBookRequest.validateForQuery(): String? {
    return ExerciseBookValidator.validateForQuery(this)
}

/**
 * 转换为实体对象（用于创建）
 */
fun ExerciseBookRequest.toEntity(currentUserId: Long): ExerciseBookEntity {
    return ExerciseBookRequestConverter.toEntity(this, currentUserId)
} 