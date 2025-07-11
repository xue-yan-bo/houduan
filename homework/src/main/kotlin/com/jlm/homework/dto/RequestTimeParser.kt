package com.jlm.homework.dto

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 请求时间解析器
 * 负责解析各种时间格式，包括ISO 8601格式和数组格式
 */
object RequestTimeParser {

    /**
     * 解析开始时间
     * 优先使用数组格式的第一个元素，其次使用单独的字段
     */
    fun parseStartTime(createdAtArray: Any?, fallbackStartTime: LocalDateTime?): LocalDateTime? {
        return parseCreatedAtArray(createdAtArray)?.first ?: fallbackStartTime
    }

    /**
     * 解析结束时间
     * 优先使用数组格式的第二个元素，其次使用单独的字段
     */
    fun parseEndTime(createdAtArray: Any?, fallbackEndTime: LocalDateTime?): LocalDateTime? {
        return parseCreatedAtArray(createdAtArray)?.second ?: fallbackEndTime
    }

    /**
     * 解析createdAt数组格式的时间
     * 支持ISO 8601格式：["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"]
     */
    private fun parseCreatedAtArray(createdAt: Any?): Pair<LocalDateTime, LocalDateTime>? {
        return when (createdAt) {
            is List<*> -> {
                if (createdAt.size >= 2) {
                    val startTimeStr = createdAt[0]?.toString()
                    val endTimeStr = createdAt[1]?.toString()

                    if (!startTimeStr.isNullOrBlank() && !endTimeStr.isNullOrBlank()) {
                        try {
                            val startTime = parseIsoDateTime(startTimeStr)
                            val endTime = parseIsoDateTime(endTimeStr)
                            if (startTime != null && endTime != null) {
                                Pair(startTime, endTime)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                } else null
            }
            else -> null
        }
    }

    /**
     * 解析ISO 8601格式的时间字符串
     * 支持格式：2025-06-30T16:00:00.000Z
     */
    private fun parseIsoDateTime(dateTimeStr: String): LocalDateTime? {
        return try {
            // 解析ISO 8601格式并转换为本地时间
            ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
        } catch (e: Exception) {
            try {
                // 尝试解析不带时区的格式
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e2: Exception) {
                null
            }
        }
    }
} 