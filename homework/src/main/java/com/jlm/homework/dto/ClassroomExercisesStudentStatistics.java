package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClassroomExercisesStudentStatistics {
    /**
     * 题目数量
     */
    private Integer questionTotal;
    /**
     * 总体正确率
     */
    private Double totalAccuracy;
    /**
     * 错误率最高的题目
     */
    private Integer maxWrongRateQuestion;
    /**
     * 答题概况(题号、答题率)
     */
    private Map<Integer,Double> answerOverviewMap;

    /**
     * 各题学生答案情况分析
     */
    private List<StudentAnswerDto>  studentAnswerDtoList;
}
