package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
/**
 * 练习册题 实体类
 */
@Data
@Entity
@Table(name = "exercise_book_question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExerciseBookQuestion implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 练习册ID
     */
    @Column(name = "exercise_book_id")
    private Long exerciseBookId;

    /**
     * 练习册章节ID
     */
    @Column(name = "exercise_book_chapter_id")
    private Long exerciseBookChapterId;

    /**
     * 知识点
     */
    @Column(name = "knowledge_point")
    private  String knowledgePoint;
    /**
     * 所在练习册页数
     */
    @Column(name = "page_number")
    private Integer pageNumber;
    /**
     * 大题号
     */
    @Column(name = "title_big_no")
    private String titleBigNo;
    /**
     * 小题号
     */
    @Column(name = "title_small_no")
    private String titleSmallNo;
    /**
     * 题所在坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coordinates",columnDefinition = "JSON")
    private QuestionCoordinate coordinates;
    /**
     * 题的图片截图
     */
    @Column(name = "croppedlrl")
    private String croppedUrl;
    /**
     * 题所在练习册页面图片
     */
    @Column(name = "imagelrl")
    private String sourceImageUrl;

    @Column(name = "question_context")
    private String questionContext;

    @Column(name = "question_image")
    private String questionImage;

    @Column(name = "answer_context")
    private String answerContext;

    @Column(name = "answer_image")
    private String answerImage;

    @Column(name = "subject")
    private String subject;
}
