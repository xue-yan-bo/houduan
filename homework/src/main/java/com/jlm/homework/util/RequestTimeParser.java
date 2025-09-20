package com.jlm.homework.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class RequestTimeParser {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析开始时间
     */
    public static LocalDateTime parseStartTime(Object input, LocalDateTime fallback) {
        if (input instanceof List) {
            // 对于数组格式，必须同时解析成功开始时间和结束时间才返回结果
            Pair<LocalDateTime, LocalDateTime> result = parseDateTimeArray((List<?>) input);
            if (result != null) {
                return result.startTime;
            }
            // 数组解析失败，返回null而不是fallback
            return null;
        }
        // 非数组格式，返回fallback
        return fallback;
    }

    /**
     * 解析结束时间
     */
    public static LocalDateTime parseEndTime(Object input, LocalDateTime fallback) {
        if (input instanceof List) {
            // 对于数组格式，必须同时解析成功开始时间和结束时间才返回结果
            Pair<LocalDateTime, LocalDateTime> result = parseDateTimeArray((List<?>) input);
            if (result != null) {
                return result.endTime;
            }
            // 数组解析失败，返回null而不是fallback
            return null;
        }
        // 非数组格式，返回fallback
        return fallback;
    }

    /**
     * 解析日期时间数组，实现"全有或全无"逻辑
     * 只有当两个时间都解析成功时才返回结果
     */
    private static Pair<LocalDateTime, LocalDateTime> parseDateTimeArray(List<?> array) {
        if (array == null || array.size() < 2) {
            return null;
        }

        Object startObj = array.get(0);
        Object endObj = array.get(1);
        
        if (startObj == null || endObj == null) {
            return null;
        }
        
        String startTimeStr = startObj.toString();
        String endTimeStr = endObj.toString();

        LocalDateTime startTime = parseDateTime(startTimeStr);
        LocalDateTime endTime = parseDateTime(endTimeStr);

        // 只有当两个时间都解析成功时才返回结果
        if (startTime != null && endTime != null) {
            return new Pair<>(startTime, endTime);
        }

        return null;
    }

    /**
     * 解析日期时间字符串
     */
    private static LocalDateTime parseDateTime(String dateStr) {
        try {
            // 优先尝试解析ISO 8601带时区的格式 (如：2025-06-30T16:00:00.000Z)
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException e1) {
            try {
                // 尝试解析ISO 8601本地时间格式
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                try {
                    // 尝试解析标准格式
                    return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e3) {
                    try {
                        // 尝试解析只包含日期的格式
                        return LocalDateTime.parse(dateStr + " 23:59:59", DATE_TIME_FORMATTER);
                    } catch (DateTimeParseException e4) {
                        return null;
                    }
                }
            }
        }
    }

    /**
     * 简单的内部Pair类，用于存储开始时间和结束时间
     */
    private static class Pair<S, E> {
        private final S startTime;
        private final E endTime;

        public Pair(S startTime, E endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}