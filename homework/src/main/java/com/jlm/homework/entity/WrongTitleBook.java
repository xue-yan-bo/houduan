package com.jlm.homework.entity;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description= "错题本实体类")
public class WrongTitleBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
     /**
     * 来源（作业、课堂练习）
     */
     @Column(name = "source")
     @ApiModelProperty(value ="来源（作业、课堂练习）")
     private String source;
     /**
      * 作业ID（作业）
      */
     @Column(name = "homework_publish_id")
     @ApiModelProperty(value ="发布作业ID")
     private Long homeworkPublishId;

    /**
     * 作业名称 （作业）
     */
    @Column(name = "homework_publish_name")
    @ApiModelProperty(value ="作业名称")
    private String homeworkPublishName;
     /**
     * 学生作业ID （作业）
     */
    @Column(name = "students_homework_id")
    @ApiModelProperty(value ="学生作业ID")
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
    @ApiModelProperty(value ="学生ID")
     private Long studentId;
     /**
     * 学生姓名
     */
     @Column(name = "student_name")
     @ApiModelProperty(value ="学生姓名")
     private String studentName;


     /**
      * 班级ID
      */
     @Column(name = "class_id")
     @ApiModelProperty(value ="班级ID")
     private Long classId;
     /**
      * 班级姓名
      */
     @Column(name = "class_name")
     @ApiModelProperty(value ="班级姓名")
     private String className;
     /**
     * 大题号
     */
     @Column(name = "title_big_no")
     @ApiModelProperty(value ="大题号")
     private String titleBigNo;
     /**
     * 小题号
     */
    @Column(name = "title_small_no")
    @ApiModelProperty(value ="小题号")
     private String titleSmallNo;
     /**
     * 题内容
     */
    @Column(name = "title_context")
    @ApiModelProperty(value ="题内容，如果有截图可以没有")
     private String titleContext;
     /**
     * 题答案
     */
    @Column(name = "title_answer")
    @ApiModelProperty(value ="题答案")
     private String titleAnswer;
    /**
     * 题图片路径(截图) （作业）
     */
    @Column(name = "title_image")
    @ApiModelProperty(value ="题图片路径(截图)")
    private String titleImage;
    /**
     * 题所在整个图片 （作业）
     */
    @Column(name = "source_image_url")
    @ApiModelProperty(value ="试题图片路径")
    private String sourceImageUrl;
     /**
     * 学生答案
     */
    @Column(name = "student_answer")
    @ApiModelProperty(value ="学生答案")
     private String studentAnswer;
     /**
     * 题解析
     */
    @Column(name = "parse")
    @ApiModelProperty(value ="题解析")
     private String parse;
    /**
     * 页码
     */
    @Column(name = "page_no")
    @ApiModelProperty(value ="页码")
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
}
