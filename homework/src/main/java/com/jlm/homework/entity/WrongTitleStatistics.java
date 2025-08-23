package com.jlm.homework.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

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
    @Column(name = "title_image")
    private String titleImage;
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
    private Double createDate;

    /**
     * 解析
     */
    @Column(name = "parse")
    private String parse;
}
