package com.jlm.homework.service

import com.jlm.homework.entity.HomeworkEntity
import com.jlm.homework.entity.HomeworkStatus
import com.jlm.homework.repository.HomeworkRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 作业服务类
 * 演示Spring Data JPA的业务逻辑处理
 */
@Service
@Transactional
class HomeworkService(
    private val homeworkRepository: HomeworkRepository
) {
    
    /**
     * 创建作业
     */
    fun createHomework(homework: HomeworkEntity): HomeworkEntity {
        return homeworkRepository.save(homework)
    }
    
    /**
     * 根据ID获取作业
     */
    @Transactional(readOnly = true)
    fun getHomeworkById(id: Long): HomeworkEntity? {
        return homeworkRepository.findById(id).orElse(null)
    }
    
    /**
     * 获取所有作业（分页）
     */
    @Transactional(readOnly = true)
    fun getAllHomework(pageable: Pageable): Page<HomeworkEntity> {
        return homeworkRepository.findAll(pageable)
    }
    
    /**
     * 根据状态获取作业
     */
    @Transactional(readOnly = true)
    fun getHomeworkByStatus(status: HomeworkStatus): List<HomeworkEntity> {
        return homeworkRepository.findByStatus(status)
    }
    
    /**
     * 根据教师ID获取作业
     */
    @Transactional(readOnly = true)
    fun getHomeworkByTeacher(teacherId: Long): List<HomeworkEntity> {
        return homeworkRepository.findByTeacherId(teacherId)
    }
    
    /**
     * 根据班级ID获取作业
     */
    @Transactional(readOnly = true)
    fun getHomeworkByClass(classId: Long): List<HomeworkEntity> {
        return homeworkRepository.findByClassId(classId)
    }
    
    /**
     * 搜索作业（根据标题）
     */
    @Transactional(readOnly = true)
    fun searchHomeworkByTitle(title: String): List<HomeworkEntity> {
        return homeworkRepository.findByTitleContainingIgnoreCase(title)
    }
    
    /**
     * 获取即将到期的作业
     */
    @Transactional(readOnly = true)
    fun getDueSoonHomework(): List<HomeworkEntity> {
        val now = LocalDateTime.now()
        val tomorrow = now.plusDays(1)
        return homeworkRepository.findDueSoon(now, tomorrow, HomeworkStatus.PUBLISHED)
    }
    
    /**
     * 更新作业
     */
    fun updateHomework(homework: HomeworkEntity): HomeworkEntity {
        return homeworkRepository.save(homework)
    }
    
    /**
     * 发布作业
     */
    fun publishHomework(id: Long): HomeworkEntity? {
        val homework = getHomeworkById(id)
        return homework?.let {
            val updatedHomework = it.copy(
                status = HomeworkStatus.PUBLISHED,
                updatedAt = LocalDateTime.now()
            )
            homeworkRepository.save(updatedHomework)
        }
    }
    
    /**
     * 删除作业
     */
    fun deleteHomework(id: Long): Boolean {
        return try {
            homeworkRepository.deleteById(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 统计教师的作业数量
     */
    @Transactional(readOnly = true)
    fun countHomeworkByTeacher(teacherId: Long): Long {
        return homeworkRepository.countByTeacherId(teacherId)
    }
    
    /**
     * 统计各状态的作业数量
     */
    @Transactional(readOnly = true)
    fun getHomeworkStatistics(): Map<HomeworkStatus, Long> {
        return HomeworkStatus.values().associateWith { status ->
            homeworkRepository.countByStatus(status)
        }
    }
}
