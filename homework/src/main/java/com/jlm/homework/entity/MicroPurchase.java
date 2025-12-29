package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "t_micro_purchase")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MicroPurchase  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id")
    private Long studentId;//学生ID

    @Column(name = "student_name")
    private String studentName;//学生姓名

    @Column(name = "class_id")
    private Long classId;//学生班级ID

    @Column(name = "class_name")
    private String className;//学生班级名称

    @Column(name = "micro_grade_id")
    private Long microGradeId;//微课年级
    @Column(name = "micro_grade_name")
    private String microGradeName;//微课年级名称
    @Column(name = "subject")
    private String subject;//微课科目

    @Column(name = "start_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;//开始时间
    @Column(name = "end_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;//结束时间
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;//创建时间
}
