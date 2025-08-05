package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 课堂练习学生答案 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises_student_answer")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercisesStudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生UUid
     */
    @Column(name = "student_id")
    private String studentId;
    /**
     * 学生姓名
     */
    @Column(name = "student_name")
    private String studentName;
    /**
     * 随堂练习id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;
    /**
     * v
     */
    @Column(name = "title_number")
    private Integer titleNumber;
    /**
     * 问题
     */
    @Column(name = "question_content")
    private String questionContent;
    /**
     * 答案
     */
    @Column(name = "answer")
    private String answer;
    /**
     * 知识点
     */
    @Column(name = "knowledge_point")
    private String knowledgePoint;
    /**
     * 学生答案
     */
    @Column(name = "student_answer")
    private String studentAnswer;
    /**
     * 正确标识
     */
    @Column(name = "right_flag")
    private Long rightFlag;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date create_time;
}
