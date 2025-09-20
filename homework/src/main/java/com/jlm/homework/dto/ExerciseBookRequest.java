package com.jlm.homework.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jlm.homework.entity.ExerciseBookChapter;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.ExerciseBookImage;
import com.jlm.homework.entity.ExerciseBookStatus;
import com.jlm.homework.util.RequestDataParser;
import com.jlm.homework.util.RequestTimeParser;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习册请求DTO
 * 统一用于创建、更新和查询练习册的请求数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExerciseBookRequest {
    /**
     * 练习册标题
     */
    private String title;

    /**
     * 练习册描述
     */
    private String description;

    /**
     * 学科名称（用于显示）
     */
    private String subject;

    /**
     * 学科ID
     */
    private Long subjectId;

    /**
     * 年级名称（用于显示）
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
     * 班级ID列表（多选支持）
     * 支持数组格式：[1, 2, 3] 或逗号分隔字符串格式："1,2,3"
     */
    @JsonProperty("classIds")
    private List<Long> classIds;

    /**
     * 班级名称列表（多选支持）
     * 支持数组格式：["一班", "二班"] 或逗号分隔字符串格式："一班,二班"
     * 只用于反序列化，避免与业务属性classNames冲突
     */
    @JsonProperty("classNames")
    private List<String> classNames;

    /**
     * 难度等级 (1-5)
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
     * 状态 (ACTIVE, INACTIVE, DELETED)
     */
    private ExerciseBookStatus status;

    /**
     * 图片URL列表（简化版，用于接收前端数据）
     * 支持数组格式：["url1", "url2"] 或逗号分隔字符串格式："url1,url2"
     */
    @JsonProperty("imageUrls")
    private Object imageUrls;

    /**
     * 图片信息列表（详细版，包含描述、类型等）
     */
    private List<ExerciseBookImage> images;

    // 时间查询相关字段
    /**
     * 创建时间查询 - 开始时间
     * 格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private LocalDateTime createdStartTime;

    /**
     * 创建时间查询 - 结束时间
     * 格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private LocalDateTime createdEndTime;

    /**
     * 创建时间查询 - 数组格式（前端兼容）
     * 格式：["2025-06-30T16:00:00.000Z", "2025-07-02T16:00:00.000Z"]
     * 第一个元素为开始时间，第二个元素为结束时间
     */
    @JsonProperty("createdAt")
    private Object _createdAt;

    // 查询相关字段
    /**
     * 页码（从1开始）
     */
    private int pageNum = 1;

    /**
     * 前端分页参数（从0开始，兼容前端）
     */
    @JsonProperty("page")
    private Integer _page;

    /**
     * 每页大小
     */
    private int pageSize = 10;

    /**
     * 前端每页大小参数（兼容前端）
     */
    @JsonProperty("size")
    private Integer _size;

    /**
     * 排序字段
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向 (asc/desc)
     */
    private String sortDir = "desc";

    /**
     * 练习册章节
     */
    private List<ExerciseBookChapter> exerciseBookChaprtList;

    // 构造函数
    public ExerciseBookRequest() {
    }


    /**
     * 获取解析后的图片URL列表
     */
    public List<String> getImageUrls() {
        return RequestDataParser.parseStringList(imageUrls);
    }

    /**
     * 获取解析后的班级ID列表
     */
    public List<Long> getClassIds() {
        return RequestDataParser.parseLongList(classIds, classId);
    }

    /**
     * 获取解析后的班级名称列表
     * 只做业务使用，不参与序列化/反序列化，避免Jackson冲突
     */
    public List<String> getClassNames() {
        return RequestDataParser.parseStringList(classNames);
    }

    /**
     * 获取解析后的创建时间开始时间
     */
    public LocalDateTime getParsedCreatedStartTime() {
        return RequestTimeParser.parseStartTime(_createdAt, createdStartTime);
    }

    /**
     * 获取解析后的创建时间结束时间
     */
    public LocalDateTime getParsedCreatedEndTime() {
        return RequestTimeParser.parseEndTime(_createdAt, createdEndTime);
    }

    /**
     * 获取实际页码（自动处理前端0开始的分页）
     */
    public int getActualPageNum() {
        return _page != null ? _page + 1 : pageNum;
    }

    /**
     * 获取实际每页大小
     */
    public int getActualPageSize() {
        return _size != null ? _size : pageSize;
    }

    /**
     * 复制当前对象并返回新对象
     */
    public ExerciseBookRequest copy() {
        ExerciseBookRequest copy = new ExerciseBookRequest();
        copy.title = this.title;
        copy.description = this.description;
        copy.subject = this.subject;
        copy.subjectId = this.subjectId;
        copy.grade = this.grade;
        copy.gradeId = this.gradeId;
        copy.classId = this.classId;
        copy.classIds = this.classIds;
        copy.classNames = this.classNames;
        copy.difficultyLevel = this.difficultyLevel;
        copy.creatorId = this.creatorId;
        copy.schoolId = this.schoolId;
        copy.status = this.status;
        copy.imageUrls = this.imageUrls;
        copy.images = this.images;
        copy.createdStartTime = this.createdStartTime;
        copy.createdEndTime = this.createdEndTime;
        copy._createdAt = this._createdAt;
        copy.pageNum = this.pageNum;
        copy._page = this._page;
        copy.pageSize = this.pageSize;
        copy._size = this._size;
        copy.sortBy = this.sortBy;
        copy.sortDir = this.sortDir;
        copy.exerciseBookChaprtList = this.exerciseBookChaprtList;
        return copy;
    }

    /**
     * 查询参数验证
     */
    public String validateForQuery() {
        return ExerciseBookValidator.validateForQuery(this);
    }

    /**
     * 创建参数验证
     */
    public String validateForCreate() {
        if (title == null || title.trim().isEmpty()) {
            return "练习册标题不能为空";
        }
        if (title.length() > 200) {
            return "练习册标题不能超过200个字符";
        }
        if (subjectId == null) {
            return "学科ID不能为空";
        }
        if (gradeId == null) {
            return "年级ID不能为空";
        }
        if (classId == null && (classIds == null || getClassIds().isEmpty())) {
            return "班级ID不能为空";
        }
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            return "难度等级必须在1-5之间";
        }
        return null; // 验证通过
    }

    /**
     * 更新参数验证
     */
    public String validateForUpdate() {
        if (title != null && title.trim().isEmpty()) {
            return "练习册标题不能为空";
        }
        if (title != null && title.length() > 200) {
            return "练习册标题不能超过200个字符";
        }
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            return "难度等级必须在1-5之间";
        }
        return null; // 验证通过
    }

    /**
     * 转换为实体对象
     */
    public ExerciseBookEntity toEntity(Long creatorId, Long schoolId) {
        ExerciseBookEntity entity = new ExerciseBookEntity();
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setSubject(subject);
        entity.setSubjectId(subjectId);
        entity.setGrade(grade);
        entity.setGradeId(gradeId);
        entity.setClassId(classId);
        entity.setClassIds(RequestDataParser.toJsonString(getClassIds()));
        entity.setClassNames(RequestDataParser.toJsonString(getClassNames()));
        entity.setDifficultyLevel(difficultyLevel != null ? difficultyLevel : 1);
        entity.setCreatorId(creatorId);
        entity.setSchoolId(schoolId);
        entity.setStatus(status != null ? status : ExerciseBookStatus.ACTIVE);
        
        // 处理图片
        if (images != null && !images.isEmpty()) {
            entity.setImages(RequestDataParser.toJsonString(images));
        } else if (imageUrls != null) {
            List<ExerciseBookImage> imageList = RequestDataParser.createImagesFromUrls(getImageUrls());
            entity.setImages(RequestDataParser.toJsonString(imageList));
        }

        return entity;
    }
}