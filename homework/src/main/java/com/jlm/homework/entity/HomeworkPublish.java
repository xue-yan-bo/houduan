package com.jlm.homework.entity;

import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.core.util.Json;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 作业发布 实体类
 */
@Data
@Entity
@Table(name = "homework_publish")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HomeworkPublish implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     *作业名称
     */
    @Column(name = "homework_name")
    @NotBlank(message = "作业名称不能为空")
    private String homeworkName;
    /**
     * 发布班级ID
     */
    @Column(name = "class_id")
    private Long classId;
    /**
     * 是否定时发布,1是、0否
     */
    @Column(name = "class_name")
    private String className;
    /**
     * 是否定时发布,1是、0否
     */
    @Column(name = "scheduled_release_flag")
    private Integer scheduledReleaseFlag;
    /**
     * 截止时间
     */
    @Column(name = "deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    /**
     * 试题来源, 1练习册 2题库  3模版发布
     */
    @Column(name = "test_source")
    private Integer testSource;
    /**
     * 旋转角度
     */
    @Column(name = "rotation_angle")
    private Integer rotationAngle;
    /**
     * 练习册ID
     */
    @Column(name = "exercise_book_id")
    private Long exerciseBookId;
    /**
     * 练习册名称
     */
    @Column(name = "exercise_book_name")
    private String exerciseBookName;
    /**
     * 起始页码
     */
    @Column(name = "start_page")
    private Integer startPage;
    /**
     * 结束页码
     */
    @Column(name = "end_page")
    private Integer endPage;
    /**
     * 发布时间
     */
    @Column(name = "publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    /**
     * 发布状态，0未发布  1已发布  2已过期
     */
    @Column(name = "publish_status")
    private Integer publishStatus;
    /**
     * 发布人ID
     */
    @Column(name = "user_id")
    private Long userId;
    /**
     * 删除标识
     */
    @Column(name = "delete_flag")
    private Integer deleteFlag;

    /**
     * 题目图片url
     */
    @Column(name = "topic_images", columnDefinition = "JSON")
    private String topicImages;
}
