package com.jlm.homework.repository;

import com.jlm.homework.dto.StudentsHomeworkSimpleDTO;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface StudentsHomeworkNewRepository extends JpaRepository<StudentsHomeworkNew, Long>, JpaSpecificationExecutor<StudentsHomeworkNew> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.start_time = ?2 WHERE h.id = ?1", nativeQuery = true)
    void saveStartTime(Long homeworkId, Date date);
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.deadline = ?2,h.submit_status = ?3 WHERE h.homework_publish_id = ?1", nativeQuery = true)
    void updateDeadline(Long homeworkPublishId,Date deadline,Integer submitStatus);
}
