package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassroomExercisesStudentAnswerRepository extends JpaRepository<ClassroomExercisesStudentAnswer,Long> , JpaSpecificationExecutor<ClassroomExercisesStudentAnswer> {
}
