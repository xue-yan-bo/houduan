package com.jlm.homework.controller;

import com.jlm.homework.dto.ExerciseBookRequest;
import com.jlm.homework.dto.ExerciseBookResponse;
import com.jlm.homework.entity.ExerciseBookChapter;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.ExerciseBookImage;
import com.jlm.homework.exception.ParameterNewException;
import com.jlm.homework.exception.ResourceNotFoundNewException;
import com.jlm.homework.service.ExerciseBookServer;
import com.jlm.homework.service.UserService;
import com.jlm.homework.service.impl.ExerciseBookChapterServiceIpml;
import com.jlm.homework.util.RequestDataParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 练习册控制器
 * 提供练习册相关的REST接口
 */
@Tag(name = "练习册管理", description = "练习册的创建、查询、更新、删除等操作，支持图片上传")
@RestController
@RequestMapping("/api/exercise-book")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "adminToken")
public class ExerciseBookController {

    private final ExerciseBookServer exerciseBookService;
    private final ExerciseBookChapterServiceIpml exerciseBookChapterServer;
    private final UserService userService;

    @Autowired
    public ExerciseBookController(
            ExerciseBookServer exerciseBookService,
            ExerciseBookChapterServiceIpml exerciseBookChapterServer,
            UserService userService) {
        this.exerciseBookService = exerciseBookService;
        this.exerciseBookChapterServer = exerciseBookChapterServer;
        this.userService = userService;
    }

    /**
     * 练习册列表查询接口（动态多条件，分页、排序）
     * POST /api/exercise-book/list
     * 前端只需请求此接口即可，支持所有查询条件
     *
     * 支持的查询条件：
     * - title: 标题模糊查询
     * - subject/subjectId: 学科查询
     * - grade/gradeId: 年级查询
     * - classId/classIds: 班级查询（支持单个或多个）
     * - difficultyLevel: 难度等级查询
     * - creatorId: 创建者查询
     * - status: 状态查询
     * - createdStartTime: 创建时间开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * - createdEndTime: 创建时间结束时间（格式：yyyy-MM-dd HH:mm:ss）
     *
     * 分页和排序：
     * - pageNum: 页码（从1开始，默认1）
     * - pageSize: 每页大小（默认10，最大100）
     * - sortBy: 排序字段（默认createdAt）
     * - sortDir: 排序方向（asc/desc，默认desc）
     */
    @PostMapping("/list")
    public Map<String, Object> list(@RequestBody ExerciseBookRequest request) {
        String validationError = request.validateForQuery();
        if (validationError != null) {
            throw new ParameterNewException(validationError);
        }

        // 租户隔离：自动添加当前学校ID作为查询条件
        Long currentSchoolId = null;
        if(request.getSchoolId()!=null){
            currentSchoolId = request.getSchoolId();
        }else {
            currentSchoolId = userService.getCurrentSchoolId();
        }
        ExerciseBookRequest requestWithSchoolId = request.copy();
        if(currentSchoolId.compareTo(1000L)!=0) {
            requestWithSchoolId.setSchoolId(currentSchoolId);
        }
        Page<ExerciseBookEntity> page = exerciseBookService.searchExerciseBooks(requestWithSchoolId);
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotalElements());
        result.put("rows", ExerciseBookResponse.fromList(page.getContent()));
        return result;
    }

    /**
     * 根据ID获取练习册详情
     * GET /api/exercise-book/{id}
     */
    @GetMapping("/{id}")
    public ExerciseBookResponse findById(@PathVariable Long id) {
        ExerciseBookEntity entity = exerciseBookService.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }

        // 租户隔离：检查练习册是否属于当前学校
        /*Long currentSchoolId = userService.getCurrentSchoolIdSafely();
        if (!entity.getSchoolId().equals(currentSchoolId)) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }*/

        List<ExerciseBookChapter> exerciseBookChapterList = exerciseBookChapterServer.getByExerciseBookId(entity.getId());
        ExerciseBookResponse response = ExerciseBookResponse.from(entity);
        response.setExerciseBookChaprtList(exerciseBookChapterList);
        return response;
    }

    /**
     * 创建练习册
     * POST /api/exercise-book
     * 支持多班级关联，可以通过classIds字段传递多个班级ID
     */
    @PostMapping
    public ExerciseBookResponse create(@RequestBody ExerciseBookRequest request) {
        // 参数验证
        String validationError = request.validateForCreate();
        if (validationError != null) {
            throw new ParameterNewException(validationError);
        }

        // 安全获取当前登录用户ID（如果获取失败会使用默认用户ID）
        Long currentUserId = userService.getCurrentUserIdSafely();

        // 租户隔离：获取当前学校ID
        Long currentSchoolId = null;
        if(request.getSchoolId()!=null){
            currentSchoolId = request.getSchoolId();
        }else {
            currentSchoolId = userService.getCurrentSchoolIdSafely();
        }

        // 转换为实体并保存（toEntity方法已经处理了多班级ID的JSON存储）
        ExerciseBookEntity exerciseBook = request.toEntity(currentUserId, currentSchoolId);
        ExerciseBookEntity savedEntity = exerciseBookService.save(exerciseBook);

        List<ExerciseBookChapter> exerciseBookChaprtList = request.getExerciseBookChaprtList();
        if (exerciseBookChaprtList != null) {
            exerciseBookChaprtList.forEach(exerciseBookChaprt->{
                exerciseBookChaprt.setExerciseBookId(savedEntity.getId());
            });
        }
        exerciseBookChapterServer.saveList(exerciseBookChaprtList);
        return ExerciseBookResponse.from(savedEntity);
    }

    /**
     * 更新练习册
     * PUT /api/exercise-book/{id}
     */
    @PutMapping("/{id}")
    public ExerciseBookResponse update(
            @PathVariable Long id,
            @RequestBody ExerciseBookRequest request) {
        // 检查练习册是否存在
        ExerciseBookEntity existing = exerciseBookService.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }

        // 租户隔离：检查练习册是否属于当前学校
        Long currentSchoolId = null;
        if(request.getSchoolId()!=null){
            currentSchoolId = request.getSchoolId();
        }else {
            currentSchoolId = userService.getCurrentSchoolIdSafely();
        }
        if (!existing.getSchoolId().equals(currentSchoolId)) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }

        // 参数验证
        String validationError = request.validateForUpdate();
        if (validationError != null) {
            throw new ParameterNewException(validationError);
        }

        // 更新字段（只更新非空字段，schoolId保持不变）
        ExerciseBookEntity updated = new ExerciseBookEntity();
        updated.setId(existing.getId());
        updated.setTitle(request.getTitle() != null ? request.getTitle() : existing.getTitle());
        updated.setDescription(request.getDescription() != null ? request.getDescription() : existing.getDescription());
        updated.setSubject(request.getSubject() != null ? request.getSubject() : existing.getSubject());
        updated.setSubjectId(request.getSubjectId() != null ? request.getSubjectId() : existing.getSubjectId());
        updated.setGrade(request.getGrade() != null ? request.getGrade() : existing.getGrade());
        updated.setGradeId(request.getGradeId() != null ? request.getGradeId() : existing.getGradeId());
        updated.setClassId(request.getClassId() != null ? request.getClassId() : existing.getClassId());
        updated.setDifficultyLevel(request.getDifficultyLevel() != null ? request.getDifficultyLevel() : existing.getDifficultyLevel());
        updated.setStatus(request.getStatus() != null ? request.getStatus() : existing.getStatus());
        updated.setSchoolId(existing.getSchoolId()); // 保持不变
        updated.setCreatorId(existing.getCreatorId());
        updated.setCreatedAt(existing.getCreatedAt());

        // 处理图片更新
        if (request.getImages() != null || !request.getImageUrls().isEmpty()) {
            List<ExerciseBookImage> imageList;
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                imageList = request.getImages();
            } else if (!request.getImageUrls().isEmpty()) {
                imageList = RequestDataParser.createImagesFromUrls(request.getImageUrls());
            } else {
                imageList = List.of();
            }
            updated.withImages(imageList);
        } else {
            updated.setImages(existing.getImages());
        }

        // 处理班级ID和名称列表的更新
        if (!request.getClassIds().isEmpty()) {
            if (!request.getClassNames().isEmpty() && request.getClassNames().size() == request.getClassIds().size()) {
                // 如果班级名称列表存在且与ID列表长度一致，同时更新ID和名称
                updated.withClassIdsAndNames(request.getClassIds(),request.getClassNames());
            } else {
                // 否则只更新ID列表
                updated.withClassIds(request.getClassIds());
            }
        } else {
            updated.setClassIds(existing.getClassIds());
            updated.setClassNames(existing.getClassNames());
        }

        ExerciseBookEntity savedEntity = exerciseBookService.save(updated);
        List<ExerciseBookChapter> exerciseBookChaprtList = request.getExerciseBookChaprtList();
        if (exerciseBookChaprtList != null) {
            for (ExerciseBookChapter exerciseBookChaprt : exerciseBookChaprtList) {
                exerciseBookChaprt.setExerciseBookId(savedEntity.getId());
            }
        }
        exerciseBookChapterServer.saveList(exerciseBookChaprtList);
        return ExerciseBookResponse.from(savedEntity);
    }

    /**
     * 删除练习册
     * DELETE /api/exercise-book/{id}
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        // 检查练习册是否存在
        ExerciseBookEntity entity = exerciseBookService.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }

        // 租户隔离：检查练习册是否属于当前学校
        /*Long currentSchoolId = userService.getCurrentSchoolIdSafely();
        if (!entity.getSchoolId().equals(currentSchoolId)) {
            throw new ResourceNotFoundNewException("练习册不存在，ID: " + id);
        }*/

        exerciseBookService.deleteById(id);
        return "删除成功";
    }
    @GetMapping("/getKnowledge")
    @Operation(summary = "根据章节查询知识点")
    public List<String> findKnowledgePointByChapter(String chapter) {
        return exerciseBookChapterServer.findKnowledgePointByChapter(chapter);
    }

}