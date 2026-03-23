package com.jlm.homework.repository;

import com.jlm.homework.dto.StudentsHomeworkSimpleDTO;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface StudentsHomeworkNewRepository extends JpaRepository<StudentsHomeworkNew, Long>, JpaSpecificationExecutor<StudentsHomeworkNew> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.start_time = ?2 WHERE h.id = ?1", nativeQuery = true)
    void saveStartTime(Long homeworkId, Date date);
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.deadline = ?2,h.submit_status = ?3 WHERE h.homework_publish_id = ?1 and h.submit_status != 1", nativeQuery = true)
    void updateDeadline(Long homeworkPublishId,Date deadline,Integer submitStatus);
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.homework_publish_name=?2,h.topic_images=?3,h.deadline=?4,h.daily_practice_id=?5, h.daily_practice_name = ?6,h.daily_practice_preview = ?7,h.chapter = ?8,h.knowledge_point = ?9,h.submit_status = ?10 WHERE h.homework_publish_id = ?1", nativeQuery = true)
    public void updateByPublishId(Long homeworkPublishId, String homeworkName, String topicImagesStr, Date deadline, Long dailyPracticeld, String dailyPracticeName, String dailyPracticePreview, String chapter, String knowledgePoint,Integer submitStatus);
    @Modifying
    @Transactional
    @Query(value = "UPDATE students_homework_new h SET h.submit_status = null WHERE h.homework_publish_id = ?1", nativeQuery = true)
    void updateSubmietNull(Long homeworkPublishId);

    @Query(value = "SELECT homework_publish_id, COUNT(*) FROM students_homework_new WHERE homework_publish_id IN ?1 AND submit_time IS NOT NULL GROUP BY homework_publish_id", nativeQuery = true)
    List<Object[]> countSubmittedByHomeworkPublishIds(List<Long> homeworkPublishIds);
}
