package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业发布 实体类
 */
@Data
@Entity
@Table(name = "classroom_exercises")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomExercises {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     *作业名称
     */
    @Column(name = "homework_name")
    private String homeworkName;
    /**
     * 学校
     */
    @Column(name = "school_id")
    private Long schoolId;
    /**
     * 年级ID
     */
    @Column(name = "grade_id")
    private Long gradeId;
    /**
     * 年级
     */
    @Column(name = "grade_name")
    private String gradeName;
    @Transient
    private List<Long> classIds;

    @Transient
    private List<String> classNames;




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
    private String userId;
    /**
     * 删除标识
     */
    @Column(name = "delete_flag")
    private Integer deleteFlag;

    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;

    /**
     * 章节
     */
    @Column(name = "chapter")
    private String chapter;
    /**
     * 知识点
     */
    @Column(name = "knowledg_piont")
    private String knowledgPiont;

    @Transient
    List<ClassroomExercisesQuestion> questionList;
}
