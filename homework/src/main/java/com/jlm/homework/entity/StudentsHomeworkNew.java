package com.jlm.homework.entity;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 学生作业 实体类
 */
@Data
@Entity
@Table(name = "students_homework_new")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentsHomeworkNew implements Serializable {
    private static final long serialVersionUID = -1L;
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
     * 年级
     */
    @Column(name = "grade")
    private String grade;
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
     * 开始做作业时间
     */
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

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
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 审批状态(0待提交、1待审批、2已审批、3订正待提交、4订正待审批、5订正已审批)
     */
    @Column(name = "audit_status")
    private Integer auditStatus;

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
    @Column(name = "teacher_audit_suggest")
    private String teacherAuditSuggest;
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
    /**
     * 截止时间
     */
    @Column(name = "deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    /**
     * 老师审批标识坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_logo_coordinate", columnDefinition = "JSON")
    private List<AuditLogoCoordinate> auditLogoCoordinate;
    /**
     * 老师审批坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_coordinate", columnDefinition = "JSON")
    private List<AuditLogoCoordinate> auditCoordinate;

    /**
     * 老师批注坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comments_coordinate", columnDefinition = "JSON")
    private List<CommentsCoordinate> commentsCoordinate;

    /**
     * 老师批注文本
     */
    @Column(name = "comment_text")
    private String commentText;

    /**
     * 章节
     */
    @Column(name = "chapter")
    private String chapter;
    /**
     * 知识点
     */
    @Column(name = "knowledge_point")
    private String knowledgePoint;

    @Transient
    private List<String> topicImages;
    /**
     * 题目图片
     */
    @Column(name = "topic_images")
    private String topicImagesStr;

    public List<String> getTopicImages() {
        if(!StringUtils.isEmpty(topicImagesStr)){
            topicImages = Arrays.asList(topicImagesStr.split(" ,"));
        }
        return topicImages;
    }
    public void setTopicImagesStr(String topicImagesStr) {
        if(topicImages!=null&&!topicImages.isEmpty()){
            this.topicImagesStr = String.join(" ,", topicImages);
        }else{
            this.topicImagesStr = topicImagesStr;
        }
    }

    /**
     * 老师2次审批标识坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_logo_coordinate2", columnDefinition = "JSON")
    private List<AuditLogoCoordinate> auditLogoCoordinate2;
    /**
     * 老师2次审批坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_coordinate2", columnDefinition = "JSON")
    private List<AuditLogoCoordinate> auditCoordinate2;

    /**
     * 老师2次批注坐标
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comments_coordinate2", columnDefinition = "JSON")
    private List<CommentsCoordinate> commentsCoordinate2;

    /**
     * 老师2次批注文本
     */
    @Column(name = "comment_text2")
    private String commentText2;

    /**
     *  1需要订正 2 学生订正完成 3老师再次审批
     */
    @Column(name = "emend_status")
    private Integer emendStatus;


    /**
     * 学生写作记录
     */
    @Transient
    private List<HomeworkStudentWriteData> studentWriteDataList;

    /**
     * 学生作业订正
     */
    @Transient
    private List<HomeworkStudentWriteData> studentWriteDataList2;

    /**
     * 每日一练ID
     */
    @Column(name = "daily_practice_id")
    private Long dailyPracticeld;
    /**
     * 每日一练名称
     */
    @Column(name = "daily_practice_name")
    private String dailyPracticeName;
    /**
     * 每日一练试题文档
     */
    @Column(name = "daily_practice_preview")
    private String dailyPracticePreview;

    /**
     * 分数
     */
    @Column(name = "score")
    private Double score;
    /**
     * AI批阅结果
     */
    @Column(name = "ai_audit")
    private String aiAudit;
    /**
     * AI批阅订正结果
     */
    @Column(name = "ai_audit2")
    private String aiAudit2;


    /**
     * 订正提交文件路径
     */
    @Column(name = "submit_file_url2")
    private String submitFileUrl2;
    /**
     * 作业相关文件
     */
    @Transient
    private List<StudentsHomeworkCorrect> homeworkCorrectList;

    /**
     * 作业相关文件
     */
    @Transient
    private List<StudentsHomeworkCorrect> homeworkCorrectList2;
}
