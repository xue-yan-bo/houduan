package com.jlm.homework.repository;

import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.ExerciseBookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

/**
 * 练习册数据访问层
 * 继承JpaSpecificationExecutor支持动态条件查询
 */
public interface ExerciseBookRepo extends JpaRepository<ExerciseBookEntity, Long>, JpaSpecificationExecutor<ExerciseBookEntity> {

    /**
     * 根据标题模糊查询练习册
     */
    List<ExerciseBookEntity> findByTitleContainingIgnoreCase(String title);

    /**
     * 根据状态查询练习册
     */
    List<ExerciseBookEntity> findByStatus(ExerciseBookStatus status);

    /**
     * 根据学科分页查询练习册
     */
    Page<ExerciseBookEntity> findBySubject(String subject, Pageable pageable);

    /**
     * 根据年级查询练习册
     */
    List<ExerciseBookEntity> findByGrade(String grade);

    /**
     * 根据创建者ID分页查询练习册
     */
    Page<ExerciseBookEntity> findByCreatorId(Long creatorId, Pageable pageable);

    /**
     * 根据难度等级查询练习册
     */
    List<ExerciseBookEntity> findByDifficultyLevel(Integer difficultyLevel);

    /**
     * 统计指定状态的练习册数量
     */
    Long countByStatus(ExerciseBookStatus status);
}