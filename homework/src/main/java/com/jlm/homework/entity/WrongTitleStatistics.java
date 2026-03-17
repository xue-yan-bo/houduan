package com.jlm.homework.entity;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jlm.homework.util.StringUtils;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Date;

/**
 * 学生作业统计 实体类
 */
@Data
@Entity
@Table(name = "wrong_title_statistics")
public class WrongTitleStatistics implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 来源
     */
    @Column(name = "source")
    private String source;
    /**
     * 发布作业ID
     */
    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;
    /**
     * 发布作业名称
     */
    @Column(name = "homework_publish_name")
    private String homeworkPublishName;
    /**
     * 题图片路径
     */
    @Column(name = "class_id")
    private Long classId;

    /**
     * 题图片路径
     */
    @Column(name = "question_id")
    private Long questionId;
    /**
     * 题图片路径
     */
    @Column(name = "title_image")
    private String titleImage;
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
     * 内容
     */
    @Column(name = "title_context")
    private String titleContext;
    /**
     * 页码
     */
    @Column(name = "page_no")
    private Integer pageNo;
    /**
     * 错误学生数
     */
    @Column(name = "wrong_student_num")
    private Integer wrongStudentNum;
    /**
     * 答题总人数
     */
    @Column(name = "answer_total")
    private Integer answerTotal;
    /**
     * 错误率
     */
    @Column(name = "wrong_rate")
    private Double wrongRate;
    /**
     * 创建时间
     */
    @Column(name = "create_date")
    private Date createDate;

    /**
     * 解析
     */
    @Column(name = "parse")
    private String parse;

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
     * 试题类型
     */
    @Column(name = "question_type")
    private String questionType;

    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;

    public String getTitleContext() {
        if(StringUtils.isNotEmpty(titleContext)&&titleContext.contains("\\")){
            return titleContext.replace("\\","");
        }
        return titleContext;
    }

    public String getParse() {
        if(StringUtils.isNotEmpty(parse)&&parse.contains("\\")){
            return parse.replace("\\","");
        }
        return parse;
    }
}
