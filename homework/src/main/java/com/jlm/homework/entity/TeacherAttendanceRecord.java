package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 老师考勤记录表 实体类
 */
@Data
@Entity
@Table(name = "teacher_attendance_record")
public class TeacherAttendanceRecord {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      /**
       * 考勤日期
       */
      @Column(name = "day")
      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
      private Date day;
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
     * 考勤开始时间
     */
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
      /**
     * 考勤结束时间
     */
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
      /**
     * 总共学生数
     */
    @Column(name = "student_sum")
    private Integer studentSum;
      /**
     * 签到人数
     */
    @Column(name = "sign_num")
    private Integer signNum;
      /**
     * 未签到人数
     */
    @Column(name = "unsign_num")
    private Integer unsignNum;
      /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 学生签到列表
     */
    @Transient
    private List<StudentSignRecord> studentSignRecordList;
}
