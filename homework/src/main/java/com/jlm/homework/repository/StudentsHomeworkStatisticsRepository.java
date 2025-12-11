package com.jlm.homework.repository;

import com.jlm.homework.dto.HomeworkRightRate;
import com.jlm.homework.entity.StudentsHomeworkStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface
StudentsHomeworkStatisticsRepository extends JpaRepository<StudentsHomeworkStatistics,Long>, JpaSpecificationExecutor<StudentsHomeworkStatistics> {
    @Query(value = "SELECT\n" +
            "\t classes_id as classId,\n" +
            "\t grade as grade,\n" +
            "\t count( 1 ) as totalNum,\n" +
            "\t SUM(IF( is_correct = 1, 1, 0 )) as rightNum,\n" +
            "\t SUM(IF( is_correct = 0, 1, 0 )) as  errorNum\n" +
            "FROM\n" +
            "\t question_analysis q\n" +
            "\t WHERE school_id = ?1 \n" +
            "\t GROUP BY classes_id,grade", nativeQuery = true)
    List<Map<String, Object>> getRightRate(Long schoolId);
    
    @Query(value = "SELECT\n" +
            "\t count( 1 ) as totalNum,\n" +
            "\t SUM(IF( is_correct = 1, 1, 0 )) as rightNum,\n" +
            "\t SUM(IF( is_correct = 0, 1, 0 )) as  errorNum\n" +
            "FROM\n" +
            "\t question_analysis q\n" +
            "\t WHERE school_id = ?1 ", nativeQuery = true)
    Map<String, Object> getRightRatetotal(Long schoolId);
}
