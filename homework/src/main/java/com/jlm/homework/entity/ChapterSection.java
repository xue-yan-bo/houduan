package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 章节 实体类
 */
@Data
@Entity
@Table(name = "chapter_section")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChapterSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     *年级
     */
    @Column(name = "code")
    private String code;
    /**
     *年级
     */
    @Column(name = "grade")
    private String grade;
    /**
     *科目
     */
    @Column(name = "subject")
    private String subject;
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

}
