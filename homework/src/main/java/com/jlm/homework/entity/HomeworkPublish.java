package com.jlm.homework.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "homework_publish")
public class HomeworkPublish {
    @Id
    private Long id;
    /**
     *作业名称
     */
    private String homeworkName;
    /**
     * 发布班级ID
     */
    private Long classId;
    /**
     * 是否定时发布,1是、0否
     */
    private String className;
    /**
     * 是否定时发布,1是、0否
     */
    private Integer scheduledReleaseFlag;
    /**
     * 截止时间
     */
    private Date deadline;
    /**
     * 试题来源, 1练习册 2题库  3模版发布
     */
    private Integer testSource;
    /**
     * 旋转角度
     */
    private Integer rotationAngle;
    /**
     * 练习册ID
     */
    private Long exerciseBookId;
    /**
     * 练习册名称
     */
    private String exerciseBookName;
    /**
     * 起始页码
     */
    private Integer startPage;
    /**
     * 结束页码
     */
    private Integer endPage;
    /**
     * 发布时间
     */
    private Date publishTime;
    /**
     * 发布状态，0未发布  1已发布  2已过期
     */
    private Integer publishStatus;
    /**
     * 发布人ID
     */
    private String userId;
    /**
     * 删除标识
     */
    private Integer deleteFlag;
}
