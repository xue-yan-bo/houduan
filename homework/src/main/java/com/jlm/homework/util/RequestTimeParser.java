package com.jlm.homework.util;

import java.time.LocalDateTime;
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
        if (fallback != null) {
            return fallback;
        }

        if (input == null) {
            return null;
        }

        if (input instanceof List) {
            List<?> list = (List<?>) input;
            if (list.size() > 0) {
                return parseDateTime(list.get(0).toString());
            }
        } else if (input instanceof String) {
            return parseDateTime((String) input);
        }
        return null;
    }

    /**
     * 解析结束时间
     */
    public static LocalDateTime parseEndTime(Object input, LocalDateTime fallback) {
        if (fallback != null) {
            return fallback;
        }

        if (input == null) {
            return null;
        }

        if (input instanceof List) {
            List<?> list = (List<?>) input;
            if (list.size() > 1) {
                return parseDateTime(list.get(1).toString());
            }
        } else if (input instanceof String) {
            return parseDateTime((String) input);
        }
        return null;
    }

    /**
     * 解析日期时间字符串
     */
    private static LocalDateTime parseDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                // 尝试解析只包含日期的格式
                return LocalDateTime.parse(dateStr + " 23:59:59", DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                try {
                    // 尝试解析ISO格式
                    return LocalDateTime.parse(dateStr);
                } catch (DateTimeParseException e3) {
                    return null;
                }
            }
        }
    }
}