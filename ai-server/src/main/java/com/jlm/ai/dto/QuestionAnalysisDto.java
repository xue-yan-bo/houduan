package com.jlm.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 试题分析结果
 */
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuestionAnalysisDto implements Serializable {
    private Long id;
    private Long studentsHomeworkId;
    private Long homeworkPublishId;
    private Long classesId;
    private Long studentId;
    private String studentName;

    private String questionNumber;     // 题号
    private String questionType;       // 题型
    private String content;            // 题目内容
    private Double score;                 // 分值
    private String studentAnswer;      // 学生答案
    private String referenceAnswer;    // 参考答案
    private Double obtainedScore;         // 得分
    private String analysis;           // 分析
    private String knowledgePoints;    // 考察知识点
    private String bigNumber;          // 大题号
    private String smallNumber;        // 小题号
    private Boolean isCorrect;         // 是否正确
    private String grade;
    private Long schoolId;
    @Override
    public String toString() {
        return questionNumber + " [" + questionType + "] " + score + "分 - 得分: " + obtainedScore;
    }
}
