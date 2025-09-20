package com.jlm.homework.dto;

import lombok.Data;
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
    /**
     * 发布状态 0未发布、1已发布、2已过期
     */
    private Integer publishStatus;

    /**
     * 批改状态，0待批改、1已批改
     */
    private Integer auditStatus;

    private String userId;

    private Long SchoolId;
}
