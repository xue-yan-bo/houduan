package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 练习册章节 实体类
 */
@Data
@Entity
@Table(name = "exercise_book_chapter")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExerciseBookChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "exercise_book_id")
    private Long exerciseBookId;
    @Column(name = "chapter_id")
    private Long chapterId;
    @Column(name = "chapter_name")
    private String chapterName;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "JSON")
    private List<String> chapterDirectImages;
    @Transient
    private List<ExerciseBookQuestion> chapterDirectCropAreas;

    @Transient
    private List<BookKnowledgePoint> knowledgePointList;


}
