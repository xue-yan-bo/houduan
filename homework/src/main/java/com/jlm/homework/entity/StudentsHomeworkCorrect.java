package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 作业文件 实体类
 */
@Data
@Entity
@Table(name = "students_homework_correct")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentsHomeworkCorrect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * '学生作业ID
     */
    @Column(name = "students_homework_id")
    private Long studentsHomeworkId;
    /**
     * 文件url
     */
    @Column(name = "file_url")
    private String fileUrl;
    /**
     * 文件类型，录音、对话、批注图片等
     */
    @Column(name = "file_type")
    private String fileType;

    /**
     * 类型，1审批批注、2订正批注
     */
    @Column(name = "type")
    private Integer type;
    /**
     * 创建身份，1老师、2学生、3家长
     */
    @Column(name = "creater_type")
    private Integer createrType;
    /**
     * 创建人Id
     */
    @Column(name = "creater_id")
    private String createrId;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;
}
