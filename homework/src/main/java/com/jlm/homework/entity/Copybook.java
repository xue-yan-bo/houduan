package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 字帖 实体类
 */
@Data
@Entity
@Table(name = "copybook")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Copybook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 字帖名称
     */
    @Column(name = "name")
    private String name;
    /**
     * 老师id
     */
    @Column(name = "teacher_id")
    private Long teacherId;
    /**
     * 老师姓名
     */
    @Column(name = "teacher_name")
    private String teacherName;
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
    @Column(name = "font")
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
     * 状态，0未发布，1已发布
     */
    @Column(name = "status")
    private Integer status;
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
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @Transient
    private String createTimeStr;
}
