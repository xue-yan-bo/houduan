package com.jlm.homework.service

import com.jlm.homework.dto.ExerciseBookRequest
import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.entity.ExerciseBookStatus
import com.jlm.homework.repository.ExerciseBookRepo
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
     * 分页查询所有练习册
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findAll(pageable: Pageable): List<ExerciseBookEntity> {
        logger.info("分页查询练习册，页码: {}, 每页大小: {}", pageable.pageNumber, pageable.pageSize)
        val page = exerciseBookRepo.findAll(pageable)
        return page.content
    }

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

        // 直接保存，时间字段由JPA注解自动管理
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
     * 根据标题模糊查询练习册
     * @param title 标题关键词
     * @return 匹配的练习册列表
     */
    fun findByTitleContaining(title: String): List<ExerciseBookEntity> {
        logger.info("根据标题搜索练习册: {}", title)
        return exerciseBookRepo.findByTitleContainingIgnoreCase(title)
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
     * 根据状态查询练习册
     * @param status 练习册状态
     * @return 指定状态的练习册列表
     */
    fun findByStatus(status: ExerciseBookStatus): List<ExerciseBookEntity> {
        logger.info("根据状态查询练习册: {}", status)
        return exerciseBookRepo.findByStatus(status)
    }

    /**
     * 根据学科分页查询练习册
     * @param subject 学科名称
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findBySubject(subject: String, pageable: Pageable): Page<ExerciseBookEntity> {
        logger.info("根据学科查询练习册: {}", subject)
        return exerciseBookRepo.findBySubject(subject, pageable)
    }

    /**
     * 根据年级查询练习册
     * @param grade 年级
     * @return 对应年级的练习册列表
     */
    fun findByGrade(grade: String): List<ExerciseBookEntity> {
        logger.info("根据年级查询练习册: {}", grade)
        return exerciseBookRepo.findByGrade(grade)
    }

    /**
     * 根据创建者ID分页查询练习册
     * @param creatorId 创建者ID
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findByCreatorId(creatorId: Long, pageable: Pageable): Page<ExerciseBookEntity> {
        logger.info("根据创建者ID查询练习册: {}", creatorId)
        return exerciseBookRepo.findByCreatorId(creatorId, pageable)
    }

    /**
     * 根据多个条件查询练习册
     * @param title 标题关键词（可选）
     * @param subject 学科（可选）
     * @param grade 年级（可选）
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页的练习册列表
     */
    fun findByConditions(
        title: String?,
        subject: String?,
        grade: String?,
        status: ExerciseBookStatus = ExerciseBookStatus.ACTIVE,
        pageable: Pageable
    ): Page<ExerciseBookEntity> {
        logger.info("根据条件查询练习册 - 标题: {}, 学科: {}, 年级: {}, 状态: {}", title, subject, grade, status)
        return exerciseBookRepo.findByConditions(title, subject, grade, status, pageable)
    }

    /**
     * 软删除练习册（设置状态为DELETED）
     * @param id 练习册ID
     * @return 更新后的练习册实体，如果不存在返回null
     */
    fun softDelete(id: Long): ExerciseBookEntity? {
        logger.info("软删除练习册，ID: {}", id)
        val exerciseBook = findById(id) ?: return null

        val deletedEntity = exerciseBook.copy(
            status = ExerciseBookStatus.DELETED
        )

        return save(deletedEntity)
    }

    /**
     * 激活练习册
     * @param id 练习册ID
     * @return 更新后的练习册实体，如果不存在返回null
     */
    fun activate(id: Long): ExerciseBookEntity? {
        logger.info("激活练习册，ID: {}", id)
        val exerciseBook = findById(id) ?: return null

        val activatedEntity = exerciseBook.copy(
            status = ExerciseBookStatus.ACTIVE
        )

        return save(activatedEntity)
    }

    /**
     * 动态查询练习册（支持多条件）
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

        // 构建分页参数（注意：PageRequest的页码从0开始，而前端传递的pageNum从1开始）
        val pageable = PageRequest.of(request.pageNum - 1, request.pageSize, sort)

        // 调用Repository的动态查询方法
        return exerciseBookRepo.findByDynamicConditions(
            title = request.title?.takeIf { it.isNotBlank() },
            subject = request.subject?.takeIf { it.isNotBlank() },
            subjectId = request.subjectId,
            grade = request.grade?.takeIf { it.isNotBlank() },
            gradeId = request.gradeId,
            classId = request.classId,
            difficultyLevel = request.difficultyLevel,
            creatorId = request.creatorId,
            status = request.status,
            pageable = pageable
        )
    }

    /**
     * 获取练习册分页数据（返回完整分页信息）
     * @param pageable 分页参数
     * @return 分页的练习册数据
     */
    fun findAllWithPage(pageable: Pageable): Page<ExerciseBookEntity> {
        logger.info("分页查询练习册（完整信息），页码: {}, 每页大小: {}", pageable.pageNumber, pageable.pageSize)
        return exerciseBookRepo.findAll(pageable)
    }
}