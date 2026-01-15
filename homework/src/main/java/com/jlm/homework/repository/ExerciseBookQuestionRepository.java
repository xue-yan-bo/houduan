package com.jlm.homework.repository;

import com.jlm.homework.entity.ExerciseBookQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseBookQuestionRepository extends JpaRepository<ExerciseBookQuestion, Long> {
}
