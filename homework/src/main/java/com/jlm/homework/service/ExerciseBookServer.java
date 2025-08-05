package com.jlm.homework.service;

import com.jlm.homework.dto.ExerciseBookRequest;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.repository.ExerciseBookRepo;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 练习册业务服务类
 * 处理练习册相关的业务逻辑
 */
@Service
public class ExerciseBookServer {
    private final ExerciseBookRepo exerciseBookRepo;
    private final Logger logger = LoggerFactory.getLogger(ExerciseBookServer.class);

    public ExerciseBookServer(ExerciseBookRepo exerciseBookRepo) {
        this.exerciseBookRepo = exerciseBookRepo;
    }

    /**
     * 根据ID查询练习册
     * @param id 练习册ID
     * @return 练习册实体，如果不存在返回null
     */
    public ExerciseBookEntity findById(Long id) {
        logger.info("根据ID查询练习册: {}", id);
        Optional<ExerciseBookEntity> optional = exerciseBookRepo.findById(id);
        return optional.orElse(null);
    }

    /**
     * 保存练习册
     * @param exerciseBook 练习册实体
     * @return 保存后的练习册实体
     */
    public ExerciseBookEntity save(ExerciseBookEntity exerciseBook) {
        logger.info("保存练习册: {}", exerciseBook.getTitle());
        return exerciseBookRepo.save(exerciseBook);
    }

    /**
     * 根据ID删除练习册
     * @param id 练习册ID
     */
    public void deleteById(Long id) {
        logger.info("删除练习册，ID: {}", id);
        exerciseBookRepo.deleteById(id);
    }

    /**
     * 统计练习册总数
     * @return 练习册总数
     */
    public Long count() {
        logger.info("统计练习册总数");
        return exerciseBookRepo.count();
    }

    /**
     * 根据标题模糊查询练习册
     * @param title 标题关键词
     * @return 匹配的练习册列表
     */
    public List<ExerciseBookEntity> findByTitleContaining(String title) {
        logger.info("根据标题搜索练习册: {}", title);
        return exerciseBookRepo.findByTitleContainingIgnoreCase(title);
    }

    /**
     * 动态查询练习册（支持多条件）
     * 使用JPA Specification实现动态条件拼接，这是唯一的列表查询方法
     * @param request 查询条件请求对象
     * @return 分页的练习册列表
     */
    public Page<ExerciseBookEntity> searchExerciseBooks(ExerciseBookRequest request) {
        logger.info("动态查询练习册 - 条件: {}", request);
        logger.info("解析后的classIds: {}", request.getClassIds());
        logger.info("实际分页参数 - pageNum: {}, pageSize: {}", request.getActualPageNum(), request.getActualPageSize());

        // 构建排序
        Sort sort = request.getSortDir().toLowerCase().equals("desc")
                ? Sort.by(request.getSortBy()).descending()
                : Sort.by(request.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(request.getActualPageNum() - 1, request.getActualPageSize(), sort);

        // 使用JPA Specification实现动态条件拼接
        Specification<ExerciseBookEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 租户隔离：必须查询当前学校的数据
            Long schoolId = request.getSchoolId();
            if (schoolId != null) {
                predicates.add(cb.equal(root.get("schoolId"), schoolId));
            }

            // 标题模糊查询
            String title = request.getTitle();
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            // 学科ID精确查询
            Long subjectId = request.getSubjectId();
            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subjectId"), subjectId));
            }

            // 学科名称精确查询
            String subject = request.getSubject();
            if (subject != null && !subject.isBlank()) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }

            // 年级ID精确查询
            Long gradeId = request.getGradeId();
            if (gradeId != null) {
                predicates.add(cb.equal(root.get("gradeId"), gradeId));
            }

            // 年级名称精确查询
            String grade = request.getGrade();
            if (grade != null && !grade.isBlank()) {
                predicates.add(cb.equal(root.get("grade"), grade));
            }

            // 班级ID（单个，向后兼容）
            Long classId = request.getClassId();
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }

            // 多班级ID查询（classIds）- 支持JSON字段查询
            List<Long> classIds = request.getClassIds();
            if (!classIds.isEmpty()) {
                List<Predicate> classIdPredicates = new ArrayList<>();

                // 查询单个class_id字段（向后兼容）
                classIdPredicates.add(root.get("classId").in(classIds));

                // 查询JSON格式的class_ids字段
                // 使用JSON_CONTAINS函数查询JSON数组中是否包含指定的班级ID
                for (Long classIdToFind : classIds) {
                    classIdPredicates.add(cb.isTrue(
                            cb.function(
                                    "JSON_CONTAINS",
                                    Boolean.class,
                                    root.get("classIds"),
                                    cb.literal(String.valueOf(classIdToFind)),
                                    cb.literal("$")
                            )
                    ));
                }

                // 使用OR连接所有班级ID条件（只要有一个匹配就返回）
                predicates.add(cb.or(classIdPredicates.toArray(new Predicate[0])));
            }

            // 难度等级查询
            Integer difficultyLevel = request.getDifficultyLevel();
            if (difficultyLevel != null) {
                predicates.add(cb.equal(root.get("difficultyLevel"), difficultyLevel));
            }

            // 创建者ID查询
            Long creatorId = request.getCreatorId();
            if (creatorId != null) {
                predicates.add(cb.equal(root.get("creatorId"), creatorId));
            }

            // 状态查询
            Object status = request.getStatus();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 创建时间区间查询
            if (request.getParsedCreatedStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getParsedCreatedStartTime()));
            }
            if (request.getParsedCreatedEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getParsedCreatedEndTime()));
            }

            // 组合所有条件
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 使用Specification动态查询
        return exerciseBookRepo.findAll(spec, pageable);
    }

    /**
     * 获取练习册分页数据（返回完整分页信息）
     * 用于不带条件的全量分页查询
     * @param pageable 分页参数
     * @return 分页的练习册数据
     */
    public Page<ExerciseBookEntity> findAllWithPage(Pageable pageable) {
        logger.info("分页查询练习册（完整信息），页码: {}, 每页大小: {}", pageable.getPageNumber(), pageable.getPageSize());
        return exerciseBookRepo.findAll(pageable);
    }
}