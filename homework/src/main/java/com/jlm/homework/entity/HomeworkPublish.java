package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课堂练习 实体类
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
    private List<Long> classId;
    /**
     * 发布班级ID
     */
    @Column(name = "class_ids")
    private String classIds;

    public List<Long> getClassId() {
        if(!StringUtils.isEmpty(classIds)&&classIds.contains(",")){
            String[] de =classIds.split(",");
            classId = Arrays.stream(de).map(String::trim).map(Long::valueOf).collect(Collectors.toList());
        }else if(!StringUtils.isEmpty(classIds)) {
            classId = Arrays.asList(Long.parseLong(classIds));
        }
        return classId;
    }
    @Transient
    private List<String> className;

    public List<String> getClassName() {
        if(!StringUtils.isEmpty(classNames)){
            className =Arrays.asList(classNames.split(","));
        }
        return className;
    }

    /**
     * 班级名称
     */
    @Column(name = "class_names")
    private String classNames;
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
     * 试题来源, 1练习册 2题库  3每日一练
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
    private String userId;
    /**
     * 删除标识
     */
    @Column(name = "delete_flag")
    private Integer deleteFlag;

    @Transient
    private List<String> topicImages;

    /**
     * 题目图片url
     */
    @Column(name = "topic_images")
    private String topicImagesStr;

    public List<String> getTopicImages() {
        if(StringUtils.isNotEmpty(topicImagesStr)){
            topicImages = Arrays.asList(topicImagesStr.split(" ,"));
        }
        return topicImages;
    }
    public void setTopicImagesStr(String topicImagesStr) {
        if(topicImages!=null&&!topicImages.isEmpty()){
            this.topicImagesStr = String.join(" ,", topicImages);
        }else{
            this.topicImagesStr = topicImagesStr;
        }
    }
    /**
     * 批改状态，1待批改、2已批改
     */
    @Column(name = "audit_status")
    private Integer auditStatus;
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
    @Column(name = "knowledge_point")
    private String knowledgePoint;

    /**
     * 每日一练ID
     */
    @Column(name = "daily_practice_id")
    private Long dailyPracticeld;
    /**
     * 每日一练名称
     */
    @Column(name = "daily_practice_name")
    private String dailyPracticeName;
    /**
     * 每日一练试题文档
     */
    @Column(name = "daily_practice_preview")
    private String dailyPracticePreview;

    /**
     * 学生总数
     */
    @Column(name = "student_sum")
    private Integer studentSum;
    @Transient
    private Long submitNum;
}
