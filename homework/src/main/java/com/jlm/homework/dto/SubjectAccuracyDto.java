package com.jlm.homework.dto;

import lombok.Data;

@Data
public class SubjectAccuracyDto {
    /**
     * 班级ID
     */
    private Long classId;
    /**
     * 班级名称
     */
    private String className;
    /**
     * 科目
     */
    private String subject;

    /**
     * 平均正确率
     */
    private Double averageAccuracy;
}
