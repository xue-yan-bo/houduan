package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomExercisesQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassroomExercisesQuestionRepository extends JpaRepository<ClassroomExercisesQuestion,Long>, JpaSpecificationExecutor<ClassroomExercisesQuestion> {
}
