package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 课堂练习学生答题记录 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises_student_record")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercisesStudentRecord {
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
     * 开始答题时间
     */
    @Column(name = "start_time")
    private Date startTime;
    /**
     * 结束答题时间
     */
    @Column(name = "end_time")
    private Date endTime;
    /**
     * 开始标记，0未开始、1已开始
     */
    @Column(name = "start_flag")
    private Integer startFlag;
    /**
     * 结束标记，0未结束、1已结束
     */
    @Column(name = "end_flag")
    private Integer endFlag;
    /**
     * 正确率
     */
    @Column(name = "accuracy")
    private Double accuracy;
}
