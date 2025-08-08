package com.jlm.homework.dto;

import lombok.Data;

@Data
public class AverageDurationAnalyse {
    /**
     * 日期
     */
    private String day;
    /**
     * 题目数量
     */
    private Integer titleNum;
    /**
     * 班级ID
     */
    private Long classId;
    /**
     * 班级名称
     */
    private String className;
    /**
     * 知识点数量
     */
    private Integer knowledgeNum;
    /**
     * 科目
     */
    private String subject;
    /**
     * 平均时长
     */
    private Double averageDuration;

}
