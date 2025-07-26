package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 知识点 实体类
 */
@Data
@Entity
@Table(name = "knowledge_point")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgePoint {
    @Id
    private Long id;

    /**
     *科目
     */
    @Column(name = "subject")
    private String subject;
    /**
     *年级ID
     */
    @Column(name = "grade_id")
    private String gradeId;
    /**
     *年级名称
     */
    @Column(name = "grade_name")
    private String gradeName;
    /**
     *知识点
     */
    @Column(name = "knowledge_piont")
    private String knowledgePiont;
}
