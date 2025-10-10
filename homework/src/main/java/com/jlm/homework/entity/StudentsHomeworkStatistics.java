package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 学生作业统计 实体类
 */
@Data
@Entity
@Table(name = "students_homework_statistics")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentsHomeworkStatistics implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 发布作业ID
     */
    @Column(name = "homework_publish_id")
    private Long homeworkPublishId;

    /**
     * 班级ID
     */
    @Column(name = "class_id")
    private Long classId;

    /**
     * 学生总人数
     */
    @Column(name = "students_sum")
    private Integer studentsSum;
    /**
     * 已提交作业人数
     */
    @Column(name = "submit_student_num")
    private Integer submitStudentNum;
    /**
     * 未提交作业人数
     */
    @Column(name = "unsubmit_student_num")
    private Integer unsubmitStudentNum;
    /**
     * 最快完成时长
     */
    @Column(name = "fastest_duration")
    private Double fastestDuration;
    /**
     * 最慢完成时长
     */
    @Column(name = "slowest_duration")
    private Double slowestDuration;
    /**
     * 平均完成时长
     */
    @Column(name = "average_duration")
    private Double averageDuration;
    /**
     * 平均正确率
     */
    @Column(name = "average_accuracy")
    private Double averageAccuracy;
    /**
     * 提交占比
     */
    @Column(name = "submit_rate")
    private Double submitRate;
    /**
     * 未提交占比
     */
    @Column(name = "unsubmit_rate")
    private Double unsubmitRate;
    /**
     * 与上次相比
     */
    @Column(name = "compare_last")
    private Double compareLast;
    /**
     * 错题量
     */
    @Column(name = "wrong_title_num")
    private Integer wrongTitleNum;
    /**
     * 总题量
     */
    @Column(name = "title_total")
    private Integer titleTotal;
    /**
     * 平均正确率
     */
    @Column(name = "average_correctness")
    private Double averageCorrectness;
    /**
     * 最高正确率
     */
    @Column(name = "max_correctness")
    private Double maxCorrectness;
    /**
     * 最低正确率
     */
    @Column(name = "min_correctness")
    private Double minCorrectness;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 修改时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 错题本
     */
    @Transient
    private List<WrongTitleStatistics>  wrongTitleStatisticses;
}
