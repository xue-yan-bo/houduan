package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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
     *章
     */
    @Column(name = "chapter")
    private String chapter;
    /**
     *节
     */
    @Column(name = "section")
    private String section;
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
    @Column(name = "question_content")
    private String questionContent;
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
     *解析
     */
    @Column(name = "parse")
    private String parse;

    /**
     *创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    public static QuestionBank hanldKong(QuestionBank questionBank){
        if(questionBank==null){
            return null;
        }
        if(StringUtils.isEmpty(questionBank.getQuestionType())){
            questionBank.setQuestionType(null);
        }
        if(StringUtils.isEmpty(questionBank.getAnswer())){
            questionBank.setAnswer(null);
        }
        if(StringUtils.isEmpty(questionBank.getArea())){
            questionBank.setArea(null);
        }
        if(StringUtils.isEmpty(questionBank.getGrade())){
            questionBank.setGrade(null);
        }
        if(StringUtils.isEmpty(questionBank.getChapter())){
            questionBank.setChapter(null);
        }
        if(StringUtils.isEmpty(questionBank.getKnowledgePoint())){
            questionBank.setKnowledgePoint(null);
        }
        if(StringUtils.isEmpty(questionBank.getPaperType())){
            questionBank.setPaperType(null);
        }
        if(StringUtils.isEmpty(questionBank.getPopularity())){
            questionBank.setPopularity(null);
        }
        if(StringUtils.isEmpty(questionBank.getSection())){
            questionBank.setSection(null);
        }
        if(StringUtils.isEmpty(questionBank.getSubject())){
            questionBank.setSubject(null);
        }
        if(StringUtils.isEmpty(questionBank.getSemester())){
            questionBank.setSemester(null);
        }
        if(StringUtils.isEmpty(questionBank.getSource())){
            questionBank.setSource(null);
        }
        if(StringUtils.isEmpty(questionBank.getParse())){
            questionBank.setParse(null);
        }
        if(StringUtils.isEmpty(questionBank.getQuestionContent())){
            questionBank.setQuestionContent(null);
        }
        return questionBank;
    }
}
