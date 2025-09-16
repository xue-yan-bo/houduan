package com.jlm.homework.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jlm.homework.entity.ExerciseBookChapter;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.ExerciseBookStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习册响应DTO
 * 用于返回给前端的练习册数据，包含班级ID数组和名称数组
 */
public class ExerciseBookResponse {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 练习册标题
     */
    private String title;

    /**
     * 练习册描述
     */
    private String description;

    /**
     * 学科名称
     */
    private String subject;

    /**
     * 学科ID
     */
    private Long subjectId;

    /**
     * 年级名称
     */
    private String grade;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 班级ID（单个，保留向后兼容）
     */
    private Long classId;

    /**
     * 班级ID数组（前端友好格式）
     */
    private List<Long> classIds;

    /**
     * 班级名称数组（前端友好格式）
     */
    private List<String> classNames;

    /**
     * 难度等级
     */
    private Integer difficultyLevel;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 学校ID（租户标识）
     */
    private Long schoolId;

    /**
     * 状态
     */
    private ExerciseBookStatus status;

    /**
     * 图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<ExerciseBookChapter> exerciseBookChaprtList;

    // 构造函数
    public ExerciseBookResponse(Long id, String title, String description, String subject, Long subjectId,
                               String grade, Long gradeId, Long classId, List<Long> classIds,
                               List<String> classNames, Integer difficultyLevel, Long creatorId,
                               Long schoolId, ExerciseBookStatus status, List<String> imageUrls,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.subjectId = subjectId;
        this.grade = grade;
        this.gradeId = gradeId;
        this.classId = classId;
        this.classIds = classIds;
        this.classNames = classNames;
        this.difficultyLevel = difficultyLevel;
        this.creatorId = creatorId;
        this.schoolId = schoolId;
        this.status = status;
        this.imageUrls = imageUrls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.exerciseBookChaprtList = null;
    }

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public List<Long> getClassIds() { return classIds; }
    public void setClassIds(List<Long> classIds) { this.classIds = classIds; }

    public List<String> getClassNames() { return classNames; }
    public void setClassNames(List<String> classNames) { this.classNames = classNames; }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public ExerciseBookStatus getStatus() { return status; }
    public void setStatus(ExerciseBookStatus status) { this.status = status; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ExerciseBookChapter> getExerciseBookChaprtList() { return exerciseBookChaprtList; }
    public void setExerciseBookChaprtList(List<ExerciseBookChapter> exerciseBookChaprtList) { this.exerciseBookChaprtList = exerciseBookChaprtList; }

    /**
     * 从实体类转换为响应DTO
     */
    public static ExerciseBookResponse from(ExerciseBookEntity entity) {
        return new ExerciseBookResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getSubject(),
            entity.getSubjectId(),
            entity.getGrade(),
            entity.getGradeId(),
            entity.getClassId(),
            entity.getClassIdList(),
            entity.getClassNameList(),
            entity.getDifficultyLevel(),
            entity.getCreatorId(),
            entity.getSchoolId(),
            entity.getStatus(),
            entity.getImageUrls(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * 批量转换实体列表为响应DTO列表
     */
    public static List<ExerciseBookResponse> fromList(List<ExerciseBookEntity> entities) {
        return entities.stream()
                .map(ExerciseBookResponse::from)
                .toList();
    }
}