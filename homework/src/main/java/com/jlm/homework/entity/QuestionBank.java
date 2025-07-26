package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题库 实体类
 */
@Data
@Entity
@Table(name = "question_bank")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuestionBank implements Serializable {
    @Id
    private Long id;

    /**
     *题目类型
     */
    @Column(name = "question_type")
    private String questionType;
    /**
     *年级
     */
    @Column(name = "grade")
    private String grade;
    /**
     *学期
     */
    @Column(name = "semester")
    private String semester;
    /**
     *科目
     */
    @Column(name = "subject")
    private String subject;
    /**
     *知识点
     */
    @Column(name = "knowledge_point")
    private String knowledgePoint;
    /**
     *问题内容
     */
    @Column(name = "questionContent")
    private String question_content;
    /**
     *答案
     */
    @Column(name = "answer")
    private String answer;
    /**
     *来源
     */
    @Column(name = "source")
    private String source;

    /**
     *来难易程度
     */
    @Column(name = "difficulty")
    private Integer difficulty;
    /**
     *地区
     */
    @Column(name = "area")
    private String area;
    /**
     *年份
     */
    @Column(name = "year")
    private Integer year;
    /**
     *试卷类型
     */
    @Column(name = "paper_type")
    private String paperType;
    /**
     *流行度（热度）
     */
    @Column(name = "popularity")
    private String popularity;
    /**
     *创建时间
     */
    @Column(name = "create_time")
    private Date createTime;
}
