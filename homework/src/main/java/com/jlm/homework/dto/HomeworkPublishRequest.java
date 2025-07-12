package com.jlm.homework.dto;

import java.util.Date;

public class HomeworkPublishRequest {
    /**
     *作业名称
     */
    private String homeworkName;
    /**
     * 发布班级ID
     */
    private Long classId;
    /**
     * 发布时间
     */
    private Date publishTime;
    /**
     * 截止时间
     */
    private Date deadline;
    /**
     * 试题来源, 1练习册 2题库  3模版发布
     */
    private Integer testSource;
}
