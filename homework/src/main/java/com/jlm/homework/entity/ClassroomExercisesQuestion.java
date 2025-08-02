package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;

/**
 * 题库 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises_question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercisesQuestion implements Serializable {
    @Id
    private Long id;
    /**
     * 随堂练习id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;
    /**
     * v
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

}
