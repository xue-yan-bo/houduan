package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@ApiModel("学生微课状况实体")
@Data
@Entity
@Table(name = "t_student_microlecture")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentMicrolecture implements Serializable {
    private static final long serialVersionUID = 7783592844043453534L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ApiModelProperty(value = "微课Id", example = "1")
    @Column(name = "microlecture_id")
    private Long microlectureId;
    @ApiModelProperty(value = "微课名称", example = "11月1周微课")
    @Column(name = "microlecture_name")
    private String microlectureName;
    @ApiModelProperty(value = "学生ID", example = "1")
    @Column(name = "student_id")
    private Long studentId;
    @ApiModelProperty(value = "学生姓名", example = "zhang")
    @Column(name = "student_name")
    private String studentName;
    @ApiModelProperty(value = "学校ID", example = "234")
    @Column(name = "school_id")
    private Long schoolId;
    @ApiModelProperty(value = "年级ID", example = "234")
    @Column(name = "grade_id")
    private Long gradeId;
    @ApiModelProperty(value = "年级名称", example = "一年级")
    @Column(name = "grade_name")
    private String gradeName;
    @ApiModelProperty(value = "班级ID", example = "234")
    @Column(name = "class_id")
    private Long classId;
    @ApiModelProperty(value = "班级名称", example = "一班")
    @Column(name = "class_name")
    private String className;
    @ApiModelProperty(value = "学科", example = "数学")
    @Column(name = "subject")
    private String subject;
    @ApiModelProperty(value = "章节", example = "乘法运算")
    @Column(name = "chapter")
    private String chapter;
    @ApiModelProperty(value = "知识点", example = "乘法运算")
    @Column(name = "knowledge_point")
    private String knowledgePoint;
    @ApiModelProperty(value = "听课开始时间", example = "")
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @ApiModelProperty(value = "听课结束时间", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "end_time")
    private Date endTime;
    @ApiModelProperty(value = "进度条", example = "23")
    @Column(name = "progress_bar")
    private Integer progressBar;
    @ApiModelProperty(value = "听课状态，0未开始、1已开始、2已完成", example = "1")
    @Column(name = "status")
    private Integer status;
    @Column(name = "file_url")
    private String fileUrl;
    @ApiModelProperty(value = "创建时间", example = "")
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @ApiModelProperty(value = "更新时间", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time")
    private Date updateTime;
}
