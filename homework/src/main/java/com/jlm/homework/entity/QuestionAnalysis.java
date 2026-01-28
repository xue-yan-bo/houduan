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
@Table(name = "question_analysis")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuestionAnalysis implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "students_homework_id")
    private Long studentsHomeworkId;
    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;
    @Column(name = "classes_id")
    private Long classesId;
    @Column(name = "student_id")
    private Long studentId;
    @Column(name = "student_name")
    private String studentName;

    @Column(name = "question_number")
    private String questionNumber;     // 题号
    @Column(name = "question_type")
    private String questionType;       // 题型
    @Column(name = "content")
    private String content;            // 题目内容
    @Column(name = "score")
    private Double score;                 // 分值
    @Column(name = "student_answer")
    private String studentAnswer;      // 学生答案
    @Column(name = "reference_answer")
    private String referenceAnswer;    // 参考答案
    @Column(name = "obtained_score")
    private Double obtainedScore;         // 得分
    @Column(name = "analysis")
    private String analysis;           // 分析
    @Column(name = "knowledge_points")
    private String knowledgePoints;    // 考察知识点
    @Column(name = "big_number")
    private String bigNumber;          // 大题号
    @Column(name = "small_number")
    private String smallNumber;        // 小题号
    @Column(name = "is_correct")
    private Boolean isCorrect;         // 是否正确
    @Column(name = "grade")
    private String grade;
    @Column(name = "school_id")
    private Long schoolId;
    @Override
    public String toString() {
        return questionNumber + " [" + questionType + "] " + score + "分 - 得分: " + obtainedScore;
    }
}
