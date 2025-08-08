package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 课堂练习题 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises_question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercisesQuestion implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 随堂练习id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;
    /**
     * 班级
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "class_ids",columnDefinition = "JSON")
    private List<Long> classIds;
    /**
     * 班级名称
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "class_names",columnDefinition = "JSON")
    private List<String> classNames;
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
     * 来源类型，0题库、1自编、2考试题
     */
    @Column(name = "source_type")
    private String sourceType;
    /**
     * 来源是题库，题库id
     */
    @Column(name = "question_bank_id")
    private Long questionBankId;
    /**
     *来难易程度
     */
    @Column(name = "difficulty")
    private Integer difficulty;
    /**
     *解析
     */
    @Column(name = "parse")
    private String parse;
    /**
     *题目类型
     */
    @Column(name = "question_type")
    private String questionType;
    /**
     *创建时间
     */
    @Column(name = "create_time")
    private Date createTime;
    /**
     * 选项
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "JSON")
    private List<String> options;
    /**
     *科目
     */
    @Column(name = "subject")
    private String subject;

}
