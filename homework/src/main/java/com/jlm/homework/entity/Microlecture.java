package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@ApiModel("微课实体")
@Data
@Entity
@Table(name = "t_microlecture")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Microlecture implements Serializable {
    private static final long serialVersionUID = 7783592844043433534L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ApiModelProperty(value = "微课名称", example = "11月1周微课")
    @Column(name = "name")
    private String name;
    @ApiModelProperty(value = "老师ID", example = "234")
    @Column(name = "teacher_id")
    private Long teacherId;
    @ApiModelProperty(value = "老师姓名", example = "张")
    @Column(name = "teacher_name")
    private String teacherName;
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
    @ApiModelProperty(value = "文件路径", example = "http://")
    @Column(name = "file_url")
    private String fileUrl;
    @ApiModelProperty(value = "创建时间", example = "")
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
