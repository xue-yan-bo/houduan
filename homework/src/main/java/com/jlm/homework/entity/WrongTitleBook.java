package com.jlm.homework.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 错题本 实体类
 */
@Data
@Entity
@Table(name = "wrong_title_book")
public class WrongTitleBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
     /**
     * 来源
     */
     @Column(name = "source")
     private String source;
     /**
     * 学生作业ID
     */
    @Column(name = "students_homework_id")
     private Long studentsHomeworkId;
     /**
     * 练习记录ID
     */
    @Column(name = "exercises_record_id")
     private Long exercisesRecordId;
     /**
     * 学生ID
     */
    @Column(name = "student_id")
     private Long studentId;
     /**
     * 学生姓名
     */
    @Column(name = "student_name")
     private String studentName;
     /**
     * 大题号
     */
    @Column(name = "title_big_no")
     private Integer titleBigNo;
     /**
     * 小题号
     */
    @Column(name = "title_small_no")
     private Integer titleSmallNo;
     /**
     * 题内容
     */
    @Column(name = "title_context")
     private String titleContext;
     /**
     * 题答案
     */
    @Column(name = "title_answer")
     private String titleAnswer;
     /**
     * 学生答案
     */
    @Column(name = "student_answer")
     private String studentAnswer;
     /**
     * 题解析
     */
    @Column(name = "parse")
     private String parse;
     /**
     * 创建时间
     */
    @Column(name = "create_time")
     private Date createTime;
}
