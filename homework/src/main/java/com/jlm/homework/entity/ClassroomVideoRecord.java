package com.jlm.homework.entity;
/**
 * 课堂学生书写数据 实体类
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "classroom_video_record")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomVideoRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 班级id
     */
    @Column(name = "class_id")
    private Long classId;
    /**
     * 班级名称
     */
    @Column(name = "class_name")
    private String className;
    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;
    /**
     * 教师id
     */
    @Column(name = "teacher_id")
    private Long teacherId;
    /**
     * 教师名称
     */
    @Column(name = "teacher_name")
    private String teacherName;
    /**
     * 开始录制时间
     */
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 结束录制时间
     */
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 文件路径
     */
    @Column(name = "file_url")
    private String fileUrl;
    /**
     * '班级id
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
