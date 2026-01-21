package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 课堂练习学生答题记录 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises_student_record")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercisesStudentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
     * 学生头像
     */
    @Column(name = "student_image")
    private String studentImage;
    /**
     * 随堂练习id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;
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
     * 开始答题时间
     */
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 结束答题时间
     */
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 开始标记，0未开始、1已开始
     */
    @Column(name = "start_flag")
    private Integer startFlag;
    /**
     * 结束标记，0未结束、1已结束
     */
    @Column(name = "end_flag")
    private Integer endFlag;
    /**
     * 做题时长，毫秒数
     */
    @Column(name = "answer_duration")
    private Long answerDuration;
    /**
     * 正确率
     */
    @Column(name = "accuracy")
    private Double accuracy;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 学生写作记录
     */
    @Transient
    private List<ClassroomStudentWriteData> studentWriteDataList;

    @Transient
    private List<ClassroomTearcherApproveStu> tearcherApproveStuList;
}
