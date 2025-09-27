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
      * 作业ID
      */
     @Column(name = "homework_publish_id")
     private Long homeworkPublishId;

    /**
     * 作业名称
     */
    @Column(name = "homework_publish_name")
    private String homeworkPublishName;
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
     * 问题ID
     */
    @Column(name = "question_id")
    private Long questionId;
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
      * 班级ID
      */
     @Column(name = "class_id")
     private Long classId;
     /**
      * 班级姓名
      */
     @Column(name = "class_name")
     private String className;
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
     * 题图片路径
     */
    @Column(name = "title_image")
    private String titleImage;
    /**
     * 题所在练习册页面图片
     */
    @Column(name = "source_image_url")
    private String sourceImageUrl;
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
     * 页码
     */
    @Column(name = "page_no")
    private Integer pageNo;
     /**
     * 创建时间
     */
    @Column(name = "create_time")
     private Date createTime;

}
