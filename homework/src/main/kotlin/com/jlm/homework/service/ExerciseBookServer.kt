package com.jlm.homework.service

import com.jlm.homework.dto.ExerciseBookRequest
import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.repository.ExerciseBookRepo
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 练习册业务服务类
 * 处理练习册相关的业务逻辑
 */
@Service
class ExerciseBookServer(
    private val exerciseBookRepo: ExerciseBookRepo
) {

    private val logger = LoggerFactory.getLogger(ExerciseBookServer::class.java)

    /**
     * 根据ID查询练习册
     * @param id 练习册ID
     * @return 练习册实体，如果不存在返回null
     */
    fun findById(id: Long): ExerciseBookEntity? {
        logger.info("根据ID查询练习册: {}", id)
        return exerciseBookRepo.findById(id).orElse(null)
    }

    /**
     * 保存练习册
     * @param exerciseBook 练习册实体
     * @return 保存后的练习册实体
     */
    fun save(exerciseBook: ExerciseBookEntity): ExerciseBookEntity {
        logger.info("保存练习册: {}", exerciseBook.title)
        return exerciseBookRepo.save(exerciseBook)
    }

    /**
     * 根据ID删除练习册
     * @param id 练习册ID
     */
    fun deleteById(id: Long) {
        logger.info("删除练习册，ID: {}", id)
        exerciseBookRepo.deleteById(id)
    }

    /**
     * 统计练习册总数
     * @return 练习册总数
     */
    fun count(): Long {
        logger.info("统计练习册总数")
        return exerciseBookRepo.count()
    }

    /**
     * 根据标题模糊查询练习册
     * @param title 标题关键词
     * @return 匹配的练习册列表
     */
    fun findByTitleContaining(title: String): List<ExerciseBookEntity> {
        logger.info("根据标题搜索练习册: {}", title)
        return exerciseBookRepo.findByTitleContainingIgnoreCase(title)
    }

    /**
     * 动态查询练习册（支持多条件）
     * 使用JPA Specification实现动态条件拼接，这是唯一的列表查询方法
     * @param request 查询条件请求对象
     * @return 分页的练习册列表
     */
    fun searchExerciseBooks(request: ExerciseBookRequest): Page<ExerciseBookEntity> {
        logger.info("动态查询练习册 - 条件: {}", request)

        // 构建排序
        val sort = if (request.sortDir.lowercase() == "desc") {
            Sort.by(request.sortBy).descending()
        } else {
            Sort.by(request.sortBy).ascending()
        }
        val pageable = PageRequest.of(request.pageNum - 1, request.pageSize, sort)

        // 使用JPA Specification实现动态条件拼接
        val spec = Specification<ExerciseBookEntity> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            
            // 标题模糊查询
            request.title?.takeIf { it.isNotBlank() }?.let {
                predicates += cb.like(cb.lower(root.get("title")), "%" + it.lowercase() + "%")
            }
            // 学科ID精确查询
            request.subjectId?.let {
                predicates += cb.equal(root.get<Long>("subjectId"), it)
            }
            // 学科名称精确查询
            request.subject?.takeIf { it.isNotBlank() }?.let {
                predicates += cb.equal(root.get<String>("subject"), it)
            }
            // 年级ID精确查询
            request.gradeId?.let {
                predicates += cb.equal(root.get<Long>("gradeId"), it)
            }
            // 年级名称精确查询
            request.grade?.takeIf { it.isNotBlank() }?.let {
                predicates += cb.equal(root.get<String>("grade"), it)
            }
            // 班级ID（单个，向后兼容）
            request.classId?.let {
                predicates += cb.equal(root.get<Long>("classId"), it)
            }
            // 多班级ID查询（classIds）
            if (request.classIds.isNotEmpty()) {
                predicates += root.get<Long>("classId").`in`(request.classIds)
            }
            // 难度等级查询
            request.difficultyLevel?.let {
                predicates += cb.equal(root.get<Int>("difficultyLevel"), it)
            }
            // 创建者ID查询
            request.creatorId?.let {
                predicates += cb.equal(root.get<Long>("creatorId"), it)
            }
            // 状态查询
            request.status?.let {
                predicates += cb.equal(root.get<Any>("status"), it)
            }
            
            // 组合所有条件
            cb.and(*predicates.toTypedArray())
        }

        // 使用Specification动态查询
        return exerciseBookRepo.findAll(spec, pageable)
    }

    /**
     * 获取练习册分页数据（返回完整分页信息）
     * 用于不带条件的全量分页查询
     * @param pageable 分页参数
     * @return 分页的练习册数据
     */
    fun findAllWithPage(pageable: Pageable): Page<ExerciseBookEntity> {
        logger.info("分页查询练习册（完整信息），页码: {}, 每页大小: {}", pageable.pageNumber, pageable.pageSize)
        return exerciseBookRepo.findAll(pageable)
    }
}