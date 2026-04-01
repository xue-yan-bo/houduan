package com.jlm.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 试题分析结果
 */
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HomeworkPublishQuestionDto implements Serializable {
    private Long id;
    private Long homeworkPublishId;
    private String questionNumber;     // 题号
    private String questionType;       // 题型
    private String content;            // 题目内容
    private Integer score;                 // 分值
    private String referenceAnswer;    // 参考答案
    private String analysis;           // 分析
    private String knowledgePoints;    // 考察知识点
    private String bigNumber;          // 大题号
    private String smallNumber;        // 小题号

    @Override
    public String toString() {
        return "HomeworkPublishQuestion{" +
                "homeworkPublishId=" + homeworkPublishId +
                ", questionNumber='" + questionNumber + '\'' +
                ", questionType='" + questionType + '\'' +
                ", content='" + content + '\'' +
                ", score=" + score +
                ", referenceAnswer='" + referenceAnswer + '\'' +
                ", analysis='" + analysis + '\'' +
                ", knowledgePoints='" + knowledgePoints + '\'' +
                ", bigNumber='" + bigNumber + '\'' +
                ", smallNumber='" + smallNumber + '\'' +
                '}';
    }
}
