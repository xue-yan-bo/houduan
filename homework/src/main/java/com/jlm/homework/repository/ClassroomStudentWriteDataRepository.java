package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomStudentWriteDataRepository extends JpaRepository<ClassroomStudentWriteData,Long> {
    List<ClassroomStudentWriteData> findByStudentRecordIdIn(List<Long> studentRecordIds);
}
