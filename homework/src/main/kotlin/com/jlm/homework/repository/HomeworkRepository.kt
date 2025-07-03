package com.jlm.homework.repository

import com.jlm.homework.entity.HomeworkEntity
import com.jlm.homework.entity.HomeworkStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 作业Repository接口
 * 演示Spring Data JPA的各种查询方法
 */
@Repository
interface HomeworkRepository : JpaRepository<HomeworkEntity, Long> {
    
    /**
     * 根据状态查找作业
     */
    fun findByStatus(status: HomeworkStatus): List<HomeworkEntity>
    
    /**
     * 根据状态分页查找作业
     */
    fun findByStatus(status: HomeworkStatus, pageable: Pageable): Page<HomeworkEntity>
    
    /**
     * 根据教师ID查找作业
     */
    fun findByTeacherId(teacherId: Long): List<HomeworkEntity>
    
    /**
     * 根据班级ID查找作业
     */
    fun findByClassId(classId: Long): List<HomeworkEntity>
    
    /**
     * 根据科目查找作业
     */
    fun findBySubject(subject: String): List<HomeworkEntity>
    
    /**
     * 根据标题模糊查找作业
     */
    fun findByTitleContainingIgnoreCase(title: String): List<HomeworkEntity>
    
    /**
     * 查找指定日期范围内的作业
     */
    fun findByDueDateBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<HomeworkEntity>
    
    /**
     * 查找即将到期的作业（未来24小时内）
     */
    @Query("SELECT h FROM HomeworkEntity h WHERE h.dueDate BETWEEN :now AND :tomorrow AND h.status = :status")
    fun findDueSoon(
        @Param("now") now: LocalDateTime,
        @Param("tomorrow") tomorrow: LocalDateTime,
        @Param("status") status: HomeworkStatus
    ): List<HomeworkEntity>
    
    /**
     * 根据教师ID和状态查找作业
     */
    fun findByTeacherIdAndStatus(teacherId: Long, status: HomeworkStatus): List<HomeworkEntity>
    
    /**
     * 根据班级ID和状态查找作业
     */
    fun findByClassIdAndStatus(classId: Long, status: HomeworkStatus): List<HomeworkEntity>
    
    /**
     * 统计指定教师的作业数量
     */
    fun countByTeacherId(teacherId: Long): Long
    
    /**
     * 统计指定状态的作业数量
     */
    fun countByStatus(status: HomeworkStatus): Long
    
    /**
     * 自定义查询：查找指定教师在指定时间范围内创建的作业
     */
    @Query("""
        SELECT h FROM HomeworkEntity h 
        WHERE h.teacherId = :teacherId 
        AND h.createdAt BETWEEN :startDate AND :endDate
        ORDER BY h.createdAt DESC
    """)
    fun findByTeacherAndDateRange(
        @Param("teacherId") teacherId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<HomeworkEntity>
}
