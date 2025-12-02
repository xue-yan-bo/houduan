package com.jlm.homework.entity;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 练习册实体类
 * 对应数据库表 exercise_book
 */
@Data
@Entity
@Table(name = "exercise_book")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExerciseBookEntity {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 练习册标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 练习册描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 学科名称（保留用于显示）
     */
    @Column(name = "subject", length = 50)
    private String subject;

    /**
     * 学科ID
     */
    @Column(name = "subject_id")
    private Long subjectId;

    /**
     * 年级名称（保留用于显示）
     */
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * 年级ID
     */
    @Column(name = "grade_id")
    private Long gradeId;

    /**
     * 班级ID（单个，保留向后兼容）
     */
    @Column(name = "class_id")
    private Long classId;

    /**
     * 班级ID列表（JSON格式存储）
     */
    @Column(name = "class_ids", columnDefinition = "JSON")
    private String classIds;

    /**
     * 班级名称列表（JSON格式存储）
     */
    @Column(name = "class_names", columnDefinition = "JSON")
    private String classNames;

    /**
     * 难度等级 (1-5)
     */
    @Column(name = "difficulty_level")
    private Integer difficultyLevel = 1;

    /**
     * 创建者ID
     */
    @Column(name = "creator_id")
    private Long creatorId;

    /**
     * 学校ID（租户标识）
     */
    @Column(name = "school_id")
    private Long schoolId;

    /**
     * 状态 (ACTIVE, INACTIVE, DELETED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ExerciseBookStatus status = ExerciseBookStatus.ACTIVE;

    /**
     * 练习册图片列表（JSON格式存储）
     */
    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    @Transient
    private List<Long> classIdList;
    @Transient
    private List<String> classNameList;
    @Transient
    private List<String> imageUrls;

    public List<Long> getClassIdList() {
        if(StringUtils.isNotEmpty(classIds)){
            Gson gson = new Gson();
            Long[] ids= gson.fromJson(classIds,Long[].class);
            return Arrays.asList(ids);
        }else {
            return new ArrayList<>();
        }

    }
    public List<String> getClassNameList() {
        if(StringUtils.isNotEmpty(classNames)){
            Gson gson = new Gson();
            String[] ids= gson.fromJson(classNames,String[].class);
            return Arrays.asList(ids);
        }else {
            return new ArrayList<>();
        }

    }    // 单例ObjectMapper，避免重复创建
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 构造函数
    public ExerciseBookEntity() {
    }

    public ExerciseBookEntity(String title, String description, String subject,Long subjectId,String grade,Long gradeId,Long classId, Integer difficultyLevel, Long creatorId, Long schoolId,ExerciseBookStatus status) {
        this.title=title;
        this.description=description;
        this.subject=subject;
        this.subjectId=subjectId;
        this.grade=grade;
        this.gradeId=gradeId;
        this.classId= classId; // 保留向后兼容
        this.difficultyLevel=difficultyLevel==null?1:difficultyLevel;
        this.creatorId=creatorId;
        this.schoolId=schoolId;
        this.status=status == null ?  ExerciseBookStatus.ACTIVE:status;

    }


    /**
     * 获取图片列表
     */
    public List<ExerciseBookImage> getImageList() {
        try {
            if (images == null || images.isBlank()) {
                return new ArrayList<>();
            } else {
                return objectMapper.readValue(
                        images,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ExerciseBookImage.class)
                );
            }
        } catch (IOException e) {
            // 在Java中通常会记录异常日志
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 设置图片列表
     */
    public void setImageList(List<ExerciseBookImage> imageList) {
        try {
            this.images = objectMapper.writeValueAsString(imageList);
        } catch (IOException e) {
            // 在Java中通常会记录异常日志
            e.printStackTrace();
        }
    }
    /**
     * 创建带图片的练习册实体
     */
    public ExerciseBookEntity withImages(List<ExerciseBookImage> imageList) {
        if (imageList.isEmpty()) {
            this.images = null;
        }
        try {
            this.images = objectMapper.writeValueAsString(imageList);
        } catch (IOException e) {}
        return this;
    }
    /**
     * 从URL列表创建图片信息
     */
    public List<ExerciseBookImage> createImagesFromUrls(List<String> urls) {
        List<ExerciseBookImage>  imageList = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            ExerciseBookImage  image = new ExerciseBookImage();
            image.setOrder(i);
            if(i==0){
                image.setType("cover");
            }else{
                image.setType("content");
            }

            image.setUrl(urls.get(i));
            imageList.add(image);
        }
        return imageList;
    }
    public ExerciseBookEntity withClassIds(List<Long> classIdList) {
        if (classIdList.isEmpty()) {
            this.setClassIds(null);
        } else {
            String classIds=null;
            try {
                classIds =objectMapper.writeValueAsString(classIdList);
            } catch ( Exception e) {
                classIds =null;
            }
            this.setClassIds(classIds);
        }
        return this;
    }
    public ExerciseBookEntity withClassIdsAndNames(List<Long> classIds, List<String> classNames) {
        if (classIds.isEmpty()) {
            this.setClassIds(null);
        } else {
            String classIdsStr=null;
            try {
                classIdsStr =objectMapper.writeValueAsString(classIds);
            } catch ( Exception e) {
                classIdsStr =null;
            }
            this.setClassIds(classIdsStr);
        }

        if (classNames.isEmpty()) {
            this.setClassNames(null);
        } else {
            String classNamesStr=null;
            try {
                classNamesStr =objectMapper.writeValueAsString(classNames);
            } catch ( Exception e) {
                classNamesStr =null;
            }
            this.setClassNames(classNamesStr);
        }
        return this;
    }




}


