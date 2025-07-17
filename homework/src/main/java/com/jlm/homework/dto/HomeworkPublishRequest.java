package com.jlm.homework.dto;

import lombok.Data;

import java.util.Date;
@Data
public class HomeworkPublishRequest {
    /**
     *作业名称
     */
    private String homeworkName;
    /**
     * 发布班级ID
     */
    private String classIds;
    /**
     * 发布时间
     */
    private String publishTime;
    /**
     * 截止时间
     */
    private String deadline;
    /**
     * 试题来源, 1练习册 2题库  3模版发布
     */
    private Integer testSource;
}
