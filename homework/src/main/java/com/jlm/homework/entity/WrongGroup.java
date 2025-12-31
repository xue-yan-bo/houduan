package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "wrong_group")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WrongGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 类型，1学生、2班级
     */
    @Column(name = "type")
    private Integer type;
    /**
     * 组卷名称
     */
    @Column(name = "name")
    private String name;
    /**
     * 学生ID
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 学生姓名
     */
    @Column(name = "student_name")
    private String studentName;
    /**
     * 含答案文件地址
     */
    @Column(name = "file_url_on")
    private String fileUrlOn;
    /**
     * 不含答案文件地址
     */
    @Column(name = "file_url_off")
    private String fileUrlOff;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
