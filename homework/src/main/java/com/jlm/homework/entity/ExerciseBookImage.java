package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
/**
 * 练习册图片信息
 */
@Data
public class ExerciseBookImage {
    /**
     * 图片URL
     */
    @JsonProperty("url")
    private String url;

    /**
     * 图片描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 图片类型（封面图、内容图等）
     */
    @JsonProperty("type")
    private String type = "content";

    /**
     * 排序顺序
     */
    @JsonProperty("order")
    private Integer order = 0;

    // 构造函数
    public ExerciseBookImage() {
        this("", null, "content", 0);
    }

    public ExerciseBookImage(String url, String description, String type, Integer order) {
        this.url = url;
        this.description = description;
        this.type = type;
        this.order = order;
    }
}
