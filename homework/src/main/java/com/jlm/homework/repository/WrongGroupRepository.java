package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WrongGroupRepository extends JpaRepository<WrongGroup,Long>, JpaSpecificationExecutor<WrongGroup> {
    /**
     * 根据学生ID获取错题组（截至指定时间之前，且已发布）
     * @param studentId 学生ID
     * @param endTime 截至时间
     * @return 错题组列表
     */
    @Query("SELECT wg FROM WrongGroup wg WHERE wg.studentId = :studentId AND wg.status = 1 AND wg.createTime < :endTime")
    List<WrongGroup> findByStudentIdAndCreateTimeBefore(@Param("studentId") Long studentId, @Param("endTime") java.util.Date endTime);

}

