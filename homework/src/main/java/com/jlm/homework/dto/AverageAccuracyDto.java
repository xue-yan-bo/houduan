package com.jlm.homework.dto;

import lombok.Data;

import java.util.Date;
@Data
public class AverageAccuracyDto {
    /**
     * 班级ID
     */
    private Long classId;
    /**
     * 班级名称
     */
    private String className;
    /**
     * 平均正确率
     */
    private Double averageAccuracy;
    /**
     * 发布日期
     */
    private String publishDate;
}
