package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 学生签到记录表 实体类
 */
@Data
@Entity
@Table(name = "student_sign_record")
public class StudentSignRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 考勤记录ID
     */
    @Column(name = "attendance_record_id")
    private Long attendanceRecordId;
    /**
     * 老师id-UUid
     */
    @Column(name = "teacher_id")
    private String teacherId;
    /**
     * 老师名称
     */
    @Column(name = "teacher_name")
    private String teacherName;
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
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 学生名称
     */
    @Column(name = "student_name")
    private String studentName;

    /**
     * 签到时间
     */
    @Column(name = "sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;
    /**
     * 签到标记，1已签到
     */
    @Column(name = "sign_flag")
    private Integer signFlag;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
