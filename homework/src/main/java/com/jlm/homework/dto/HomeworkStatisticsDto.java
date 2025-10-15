package com.jlm.homework.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class HomeworkStatisticsDto {
    /**
     * 作业总数
     */
    private Integer homeworkNum;
    /**
     * 作业完成率
     */
    private BigDecimal compleRate;

    /**
     * 作业批改率
     */
    private BigDecimal auditRate;
    /**
     * 平均时长
     */
    private Double averageDuration;

    /**
     * 平均正确率
     */
    private Double averageAccuracy;
    /**
     * 年级作业正确率
     */
    private List<GradeDayAccuracy> gradeDayAccuracyList;
    /**
     * 作业散点图
     */
    private List<SubjectTimeNum> subjectTimeNumList;
    /**
     * 作业正批阅比率
     */
    private List<GradeAuditRate> gradeAuditRateList;
    /**
     * 作业数量分布
     */
    private Map<String,Integer> subjectNumMap;

}
