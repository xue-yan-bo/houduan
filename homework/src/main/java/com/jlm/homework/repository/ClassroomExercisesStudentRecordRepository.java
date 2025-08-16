package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomExercisesStudentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;

public interface ClassroomExercisesStudentRecordRepository extends JpaRepository<ClassroomExercisesStudentRecord, Long>, JpaSpecificationExecutor<ClassroomExercisesStudentRecord> {

}
