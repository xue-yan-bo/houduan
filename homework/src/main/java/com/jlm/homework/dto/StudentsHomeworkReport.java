package com.jlm.homework.dto;

import lombok.Data;

@Data
public class StudentsHomeworkReport {
    /**
     * 作用类型，1设计作业、2发布作业、3组题作业
     */
    private Integer homeworkType;

    /**
     * 发布作业名称
     */
    private String homeworkPublishName;

    /**
     * 年级
     */
    private String grade;
    /**
     * 班级名称
     */
    private String classesName;
    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 科目
     */
    private String subject;
    /**
     * 时长
     */
    private Double duration;
}
