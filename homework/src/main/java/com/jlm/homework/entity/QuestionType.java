package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 题型 实体类
 */
@Data
@Entity
@Table(name = "question_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuestionType {
    @Id
    private Long id;

    /**
     *题型编码
     */
    @Column(name = "question_type_code")
    private String questionTypeCode;
    /**
     *题型名称
     */
    @Column(name = "question_type_name")
    private String questionTypeName;
    /**
     *展示标识，0不展示、1展示
     */
    @Column(name = "show_flag")
    private Integer showFlag;

    /**
     *排序
     */
    @Column(name = "sort")
    private Integer sort;
}
