package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 试题分析结果
 */
@Data
@Entity
@Table(name = "homework_publish_question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HomeworkPublishQuestion implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;
    @Column(name = "question_number")
    private String questionNumber;     // 题号
    @Column(name = "question_type")
    private String questionType;       // 题型
    @Column(name = "content")
    private String content;            // 题目内容
    @Column(name = "score")
    private Integer score;                 // 分值
    @Column(name = "reference_answer")
    private String referenceAnswer;    // 参考答案
    @Column(name = "analysis")
    private String analysis;           // 分析
    @Column(name = "knowledge_points")
    private String knowledgePoints;    // 考察知识点
    @Column(name = "big_number")
    private String bigNumber;          // 大题号
    @Column(name = "small_number")
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
