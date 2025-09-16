package com.jlm.homework.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlm.homework.entity.ExerciseBookImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestDataParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析字符串列表（支持数组和逗号分隔字符串）
     */
    public static List<String> parseStringList(Object input) {
        if (input == null) {
            return new ArrayList<>();
        }

        if (input instanceof List) {
            return (List<String>) input;
        } else if (input instanceof String) {
            String str = (String) input;
            if (str.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(str.split(","));
        }
        return new ArrayList<>();
    }

    /**
     * 解析长整型列表（支持数组和逗号分隔字符串）
     */
    public static List<Long> parseLongList(Object input, Long fallback) {
        if (input == null) {
            if (fallback != null) {
                return Arrays.asList(fallback);
            }
            return new ArrayList<>();
        }

        if (input instanceof List) {
            List<?> list = (List<?>) input;
            List<Long> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number) {
                    result.add(((Number) item).longValue());
                } else if (item instanceof String) {
                    try {
                        result.add(Long.parseLong((String) item));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        } else if (input instanceof String) {
            String str = (String) input;
            if (str.trim().isEmpty()) {
                if (fallback != null) {
                    return Arrays.asList(fallback);
                }
                return new ArrayList<>();
            }

            List<Long> result = new ArrayList<>();
            String[] parts = str.split(",");
            for (String part : parts) {
                try {
                    result.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            return result;
        } else if (input instanceof Number) {
            return Arrays.asList(((Number) input).longValue());
        }
        return new ArrayList<>();
    }

    /**
     * 将对象转换为JSON字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "[]";
        }
    }

    /**
     * 从URL列表创建图片对象列表
     */
    public static List<ExerciseBookImage> createImagesFromUrls(List<String> urls) {
        List<ExerciseBookImage> images = new ArrayList<>();
        if (urls != null) {
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                if (url != null && !url.trim().isEmpty()) {
                    ExerciseBookImage image = new ExerciseBookImage();
                    image.setUrl(url);
                    image.setOrder(i);
                    images.add(image);
                }
            }
        }
        return images;
    }
}