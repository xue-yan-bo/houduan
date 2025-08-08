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
     * 班级ID
     */
    @Column(name = "class_id")
    private Long classId;
    /**
     * 班级名称
     */
    @Column(name = "class_name")
    private String className;
    /**
     * 随堂练习id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;

    /**
     * 练习问题id
     */
    @Column(name = "exercise_question_id")
    private Long exerciseQuestionId;
    /**
     * 学生答题记录id
     */
    @Column(name = "exercises_student_record_id")
    private Long exercisesStudentRecordId;
    /**
     * 题号
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
    private Integer rightFlag;
    /**
     *科目
     */
    @Column(name = "subject")
    private String subject;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;
}
