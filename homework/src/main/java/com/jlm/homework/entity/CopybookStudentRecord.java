package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 字帖 实体类
 */
@Data
@Entity
@Table(name = "copybook_student_record")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CopybookStudentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 字帖id
     */
    @Column(name = "copybook_id")
    private Long copybookId;
    /**
     * 字帖名称
     */
    @Column(name = "copybook_name")
    private String copybookName;
    /**
     * 学生id
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 学生姓名
     */
    @Column(name = "student_name")
    private String studentName;
    /**
     * 字帖内容
     */
    @Column(name = "content")
    private String content;
    /**
     * 文件url
     */
    @Column(name = "url")
    private String url;
    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;
    /**
     * 字体
     */
    @Column(name = "name")
    private String font;
    /**
     * 格式，田字纹、三线纹
     */
    @Column(name = "format")
    private String format;
    /**
     * 类型，字、词、拼音、句等
     */
    @Column(name = "type")
    private String type;
    /**
     * 学校ID
     */
    @Column(name = "school_id")
    private Long schoolId;
    /**
     * 班级ID
     */
    @Column(name = "class_id")
    private Long classId;
    /**
     * 班级名称
     */
    @Column(name = "class_name")
    private String className;
    /**
     * 年级ID
     */
    @Column(name = "grade_id")
    private Long gradeId;
    /**
     * 年级名称
     */
    @Column(name = "grade_name")
    private String gradeName;

    /**
     * 提交状态，0未提交、1已提交
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

    @Transient
    private String createTimeStr;


    /**
     * 截止时间
     */
    @Column(name = "deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    /**
     * 学生写作记录
     */
    @Transient
    private List<CopybookStudentWriteData> studentWriteDataList;
}
