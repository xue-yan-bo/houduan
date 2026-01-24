package com.jlm.homework.repository;


import com.jlm.homework.dto.FeedbackDto;
import com.jlm.homework.entity.StudentFeedback;
import jakarta.annotation.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

@Resource
public interface StudentFeedbackRepository  extends JpaRepository<StudentFeedback, Long>, JpaSpecificationExecutor<StudentFeedback> {
    @Query(value = "SELECT DATE_FORMAT(feedback_time,'%Y-%m-%d') as feedbackDate,class_id as classId,class_name as className,COUNT(id) as num FROM student_feedback \n" +
            "WHERE school_id = ?1 AND feedback_time >= ?2 AND feedback_time <= ?3 \n " +
            "GROUP BY DATE_FORMAT(feedback_time,'%Y-%m-%d'),class_id,class_name ", nativeQuery = true)
    public List<Object[]> getFeedbackDto(Long schoolId, String start, String end);
}
