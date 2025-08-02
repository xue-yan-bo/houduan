package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
@Data
@Entity
@Table(name = "book_knowledge_point")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BookKnowledgePoint {
    @Id
    private Long id;
    @Column(name = "exercise_book_id")
    private Long exerciseBookId;
    @Column(name = "exercise_book_chapter_id")
    private Long exerciseBookChapterId;
    @Column(name = "knowledge_point")
    private String knowledgePoint;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "JSON")
    private List<String> imageUrls;
    @Transient
    private List<ExerciseBookQuestion> questionList;
}
