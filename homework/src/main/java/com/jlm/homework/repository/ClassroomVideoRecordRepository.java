package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomVideoRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomVideoRecordRepository extends JpaRepository<ClassroomVideoRecord, Long> {
}
