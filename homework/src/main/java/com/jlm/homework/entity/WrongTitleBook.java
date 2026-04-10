package com.jlm.homework.entity;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;

/**
 * 错题本 实体类
 */
@Data
@Entity
@Table(name = "wrong_title_book")
public class WrongTitleBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source")
    private String source;

    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;

    @Column(name = "homework_publish_name")
    private String homeworkPublishName;

    @Column(name = "students_homework_id")
    private Long studentsHomeworkId;

    @Column(name = "exercises_record_id")
    private Long exercisesRecordId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name")
    private String className;

    @Column(name = "title_big_no")
    private String titleBigNo;

    @Column(name = "title_small_no")
    private String titleSmallNo;

    @Column(name = "title_context")
    private String titleContext;

    @Column(name = "title_answer")
    private String titleAnswer;

    
    @Column(name = "title_image")
    private String titleImage;

    
    @Column(name = "source_image_url")
    private String sourceImageUrl;

    @Column(name = "student_answer")
    private String studentAnswer;

    @Column(name = "parse")
    private String parse;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "write_data_id")
    private Long writeDataId;

    @Column(name = "knowledge_point")
    private String knowledgePoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_chart", columnDefinition = "JSON")
    private JSONObject aiChart;

    @Column(name = "command_flag")
    private Integer commandFlag;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "subject")
    private String subject;

    @Column(name = "error_count", columnDefinition = "int default 1")
    private Integer errorCount = 1;

    @Column(name = "duplicate_status", columnDefinition = "int default 0")
    private Integer duplicateStatus = 0;

    @Column(name = "duplicate_of")
    private Long duplicateOf;

    @Column(name = "exercise_book_id")
    private Long exerciseBookId;

    @Column(name = "exercise_book_question_id")
    private Long exerciseBookQuestionId;

    @Column(name = "answer_context")
    private String answerContext;

    @Column(name = "answer_image")
    private String answerImage;

    @Column(name = "ai_analysis")
    private String aiAnalysis;

    @Column(name = "grade")
    private String grade;

    @Column(name = "has_diagram", columnDefinition = "int default 0")
    private Integer hasDiagram = 0;
}