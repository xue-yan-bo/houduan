package com.jlm.homework.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 学生作业简单DTO类
 * 只包含简单基础类型字段，不包含List、JSON和longtext类型字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentsHomeworkSimpleDTO implements Serializable {
    private static final long serialVersionUID = -1L;
    
    private Long id;
    /**
     * 作用类型，1设计作业、2发布作业、3组题作业
     */
    private Integer homeworkType;
    /**
     * 发布作业Id
     */
    private Long homeworkPublishId;

    /**
     * 发布作业名称
     */
    private String homeworkPublishName;
    /**
     * 组合作业ID
     */
    private Long combinationQuestionsId;
    /**
     * 设计作业Id
     */
    private Long designId;
    /**
     * 学校Id
     */
    private Long schoolId;
    /**
     * 年级
     */
    private String grade;
    /**
     * 班级Id
     */
    private Long classesId;

    /**
     * 班级名称
     */
    private String classesName;
    /**
     * 学生Id
     */
    private Long studentId;
    /**
     * 学生姓名
     */
    private String studentName;
    /**
     * 学生uuid
     */
    private String studentUuid;
    /**
     * 提交文件路径
     */
    private String submitFileUrl;

    /**
     * 开始做作业时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 提交状态
     */
    private Integer submitStatus;
    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 审批状态(0待提交、1待审批、2已审批、3订正待提交、4订正待审批、5订正已审批)
     */
    private Integer auditStatus;

    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;
    /**
     * 错误原因
     */
    private String errorReason;
    /**
     * 建议
     */
    private String suggestion;
    /**
     * 设计文件ID
     */
    private String design_file_id;
    /**
     * 老师审批建议
     */
    private String teacherAuditSuggest;
    /**
     * 老师审批评级
     */
    private String teacherAuditLevel;

    /**
     * 科目
     */
    private String subject;
    /**
     * 正确率
     */
    private Double accuracy;
    /**
     * 班级排名
     */
    private Double classRank;
    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    
    /**
     * 老师批注文本
     */
    private String commentText;

    /**
     * 章节
     */
    private String chapter;
    /**
     * 知识点
     */
    private String knowledgePoint;

    /**
     * 1需要订正 2 学生订正完成 3老师再次审批
     */
    private Integer emendStatus;

    /**
     * 每日一练ID
     */
    private Long dailyPracticeld;
    /**
     * 每日一练名称
     */
    private String dailyPracticeName;
    
    /**
     * 分数
     */
    private Double score;


}