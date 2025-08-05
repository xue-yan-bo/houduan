package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

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
    @Transient
    private List<BookKnowledgePoint> knowledgePointList;

    public void setExerciseBookId(Long exerciseBookId) {
        this.exerciseBookId = exerciseBookId;
    }
}
