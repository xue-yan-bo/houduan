package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 学生作业 实体类
 */
@Data
@Entity
@Table(name = "students_homework_new")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentsHomeworkNew implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 作用类型，1设计作业、2发布作业、3组题作业
     */
    @Column(name = "homework_type")
    private  Integer homeworkType;
    /**
     * 发布作业Id
     */
    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;

    /**
     * 发布作业名称
     */
    @Column(name = "homework_publish_name")
    private String homeworkPublishName;
    /**
     * 组合作业ID
     */
    @Column(name = "combination_questions_id")
    private Long combinationQuestionsId;
    /**
     * 设计作业Id
     */
    @Column(name = "design_id")
    private Long designId;
    /**
     * 学校Id
     */
    @Column(name = "school_id")
    private Long schoolId;
    /**
     * 班级Id
     */
    @Column(name = "classes_id")
    private Long classesId;

    /**
     * 班级名称
     */
    @Column(name = "classes_name")
    private String classesName;
    /**
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 学生姓名
     */
    @Column(name = "student_name")
    private String studentName;
    /**
     * 学生uuid
     */
    @Column(name = "student_uuid")
    private String studentUuid;
    /**
     * 提交文件路径
     */
    @Column(name = "submit_file_url")
    private String submitFileUrl;

    /**
     * 提交状态
     */
    @Column(name = "submit_status")
    private Integer submitStatus;
    /**
     * 提交时间
     */
    @Column(name = "submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    /**
     * AI审批结果
     */
    @Column(name = "ai_audit_results")
    private String aiAuditResults;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 审批状态
     */
    @Column(name = "audit_status")
    private String auditStatus;

    /**
     * 审批时间
     */
    @Column(name = "audit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;
    /**
     * 错误原因
     */
    @Column(name = "error_reason")
    private String errorReason;
    /**
     * 建议
     */
    @Column(name = "suggestion")
    private String suggestion;
    /**
     * 设计文件ID
     */
    @Column(name = "design_file_id")
    private String design_file_id;
    /**
     * 老师审批建议
     */
    @Column(name = "teacherAuditSuggest")
    private String teacher_audit_suggest;
    /**
     * 老师审批评级
     */
    @Column(name = "teacher_audit_level")
    private String teacherAuditLevel;

    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;
    /**
     * 正确率
     */
    @Column(name = "accuracy")
    private Double accuracy;
    /**
     * 班级排名
     */
    @Column(name = "class_rank")
    private Double classRank;

}
