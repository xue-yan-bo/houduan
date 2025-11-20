package com.jlm.homework.repository;

import com.jlm.homework.entity.QuestionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestionAnalysisRepository extends JpaRepository<QuestionAnalysis,Long>, JpaSpecificationExecutor<QuestionAnalysis> {
}
