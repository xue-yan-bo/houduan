package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

/**
 * 学生反馈 实体类
 */
@Data
@Entity
@Table(name = "student_feedback")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生ID
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 学生名称
     */
    @Column(name = "student_name")
    private String studentName;
    /**
     * 班级ID
     */
    @Column(name = "class_id")
    private Long classId;
    /**
     * 班级名称
     */
    @Column(name = "class_name")
    private Long className;
    /**
     * 学科
     */
    @Column(name = "subject")
    private Long subject;
    /**
     * 反馈内容
     */
    @Column(name = "feedback_content")
    private Long feedbackContent;
    /**
     * 反馈时间
     */
    @Column(name = "feedback_time")
    private Long feedbackTime;
    /**
     * 答复内容
     */
    @Column(name = "reply_content")
    private List replyContent;
    /**
     * 答复人ID
     */
    @Column(name = "reply_id")
    private Long replyId;
    /**
     * 答复人姓名
     */
    @Column(name = "reply_name")
    private Long replyName;
    /**
     * 答复时间
     */
    @Column(name = "reply_time")
    private Long replyTime;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Long createTime;
}
