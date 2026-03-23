package com.jlm.homework.repository;

import com.jlm.homework.entity.HomeworkStudentWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface HomeworkStudentWriteDataRepository extends JpaRepository<HomeworkStudentWriteData,Long> , JpaSpecificationExecutor<HomeworkStudentWriteData> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE homework_student_write_data h SET h.offset = ?4 WHERE h.student_homework_id = ?1 and h.page_num = ?2 and h.type_n = ?3", nativeQuery = true)
    void updateOffset(Long studentHomeworkId, Integer pageNum, String type, Object offset);
}
