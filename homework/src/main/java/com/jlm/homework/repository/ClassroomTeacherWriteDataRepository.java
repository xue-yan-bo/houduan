package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomTeacherWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomTeacherWriteDataRepository extends JpaRepository<ClassroomTeacherWriteData,Long> {
}
