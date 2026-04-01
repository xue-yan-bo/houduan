package com.jlm.homework.entity;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.persistence.*;
import lombok.Data;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getHomeworkPublishId() {
        return homeworkPublishId;
    }

    public void setHomeworkPublishId(Long homeworkPublishId) {
        this.homeworkPublishId = homeworkPublishId;
    }

    public String getHomeworkPublishName() {
        return homeworkPublishName;
    }

    public void setHomeworkPublishName(String homeworkPublishName) {
        this.homeworkPublishName = homeworkPublishName;
    }

    public Long getStudentsHomeworkId() {
        return studentsHomeworkId;
    }

    public void setStudentsHomeworkId(Long studentsHomeworkId) {
        this.studentsHomeworkId = studentsHomeworkId;
    }

    public Long getExercisesRecordId() {
        return exercisesRecordId;
    }

    public void setExercisesRecordId(Long exercisesRecordId) {
        this.exercisesRecordId = exercisesRecordId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTitleBigNo() {
        return titleBigNo;
    }

    public void setTitleBigNo(String titleBigNo) {
        this.titleBigNo = titleBigNo;
    }

    public String getTitleSmallNo() {
        return titleSmallNo;
    }

    public void setTitleSmallNo(String titleSmallNo) {
        this.titleSmallNo = titleSmallNo;
    }

    public String getTitleContext() {
        return titleContext;
    }

    public void setTitleContext(String titleContext) {
        this.titleContext = titleContext;
    }

    public String getTitleAnswer() {
        return titleAnswer;
    }

    public void setTitleAnswer(String titleAnswer) {
        this.titleAnswer = titleAnswer;
    }

    public String getTitleImage() {
        return titleImage;
    }

    public void setTitleImage(String titleImage) {
        this.titleImage = titleImage;
    }

    public String getSourceImageUrl() {
        return sourceImageUrl;
    }

    public void setSourceImageUrl(String sourceImageUrl) {
        this.sourceImageUrl = sourceImageUrl;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public String getParse() {
        return parse;
    }

    public void setParse(String parse) {
        this.parse = parse;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getWriteDataId() {
        return writeDataId;
    }

    public void setWriteDataId(Long writeDataId) {
        this.writeDataId = writeDataId;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public JSONObject getAiChart() {
        return aiChart;
    }

    public void setAiChart(JSONObject aiChart) {
        this.aiChart = aiChart;
    }

    public Integer getCommandFlag() {
        return commandFlag;
    }

    public void setCommandFlag(Integer commandFlag) {
        this.commandFlag = commandFlag;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getDuplicateStatus() {
        return duplicateStatus;
    }

    public void setDuplicateStatus(Integer duplicateStatus) {
        this.duplicateStatus = duplicateStatus;
    }

    public Long getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(Long duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public Long getExerciseBookId() {
        return exerciseBookId;
    }

    public void setExerciseBookId(Long exerciseBookId) {
        this.exerciseBookId = exerciseBookId;
    }

    public Long getExerciseBookQuestionId() {
        return exerciseBookQuestionId;
    }

    public void setExerciseBookQuestionId(Long exerciseBookQuestionId) {
        this.exerciseBookQuestionId = exerciseBookQuestionId;
    }

    public String getAnswerContext() {
        return answerContext;
    }

    public void setAnswerContext(String answerContext) {
        this.answerContext = answerContext;
    }

    public String getAnswerImage() {
        return answerImage;
    }

    public void setAnswerImage(String answerImage) {
        this.answerImage = answerImage;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
}