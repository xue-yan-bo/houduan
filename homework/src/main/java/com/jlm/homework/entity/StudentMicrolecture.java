package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@Table(name = "t_student_microlecture")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentMicrolecture implements Serializable {
    private static final long serialVersionUID = 7783592844043453534L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "microlecture_id")
    private Long microlectureId;
    @Column(name = "microlecture_name")
    private String microlectureName;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "school_id")
    private Long schoolId;
    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "grade_name")
    private String gradeName;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name")
    private String className;

    @Column(name = "subject")
    private String subject;

    @Column(name = "chapter")
    private String chapter;

    @Column(name = "knowledge_point")
    private String knowledgePoint;

    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "progress_bar")
    private Integer progressBar;

    @Column(name = "status")
    private Integer status;
    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time")
    private Date updateTime;
}
