package com.jlm.homework.repository;

import com.jlm.homework.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionTypeRepository extends JpaRepository<QuestionType, Long> {
}
