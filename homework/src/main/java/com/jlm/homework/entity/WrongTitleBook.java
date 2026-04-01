package com.jlm.homework.entity;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
     * 来源（作业、课堂练习）
     */
     @Column(name = "source")
     private String source;
     /**
      * 作业ID（作业）
      */
     @Column(name = "homework_publish_id")
     private Long homeworkPublishId;

    /**
     * 作业名称 （作业）
     */
     @Column(name = "homework_publish_name")
     private String homeworkPublishName;
     /**
     * 学生作业ID （作业）
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
     * 学校ID
     */
     @Column(name = "school_id")
     private Long schoolId;
     /**
      * 班级ID
      */
    /**
     * 年级
     */
    @Column(name = "grade")
    private String grade;

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
     private String titleBigNo;
     /**
     * 小题号
     */
    @Column(name = "title_small_no")
     private String titleSmallNo;
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
     * 题图片路径(截图) （作业）
     */
    @Column(name = "title_image")
    private String titleImage;
    /**
     * 题所在整个图片 （作业）
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


    /**
     * 创建时间
     */
    @Column(name = "write_data_id")
    private Long writeDataId;
    /**
     * 知识点
     */
    @Column(name = "knowledge_point")
    private String knowledgePoint;
    /**
     * AI图
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_chart", columnDefinition = "JSON")
    private JSONObject aiChart;

    /**
     * 掌握标记，0未掌握、1已掌握
     */
    @Column(name = "command_flag")
    private Integer commandFlag;

    /**
     * 试题类型
     */
    @Column(name = "question_type")
    private String questionType;

    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;

    @Column(name = "error_count", columnDefinition = "int default 1")
    private Integer errorCount = 1;

    @Column(name = "duplicate_status", columnDefinition = "int default 0")
    private Integer duplicateStatus = 0;

    @Column(name = "duplicate_of")
    private Long duplicateOf;

}