package com.jlm.homework.repository;

import com.jlm.homework.dto.HomeworkRightRate;
import com.jlm.homework.entity.QuestionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QuestionAnalysisRepository extends JpaRepository<QuestionAnalysis,Long>, JpaSpecificationExecutor<QuestionAnalysis> {

}
