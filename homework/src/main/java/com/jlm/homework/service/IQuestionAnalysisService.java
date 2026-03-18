package com.jlm.homework.service;

import com.jlm.homework.dto.AIQuestionAnalysisDto;
import com.jlm.homework.entity.QuestionAnalysis;

import java.util.List;

public interface IQuestionAnalysisService {
    void save(QuestionAnalysis questionAnalysis);

    void saveAll(List<QuestionAnalysis> questionAnalysisList);

    List<AIQuestionAnalysisDto> findAIQuestionStatistics(Long homeworkPublishId, Long classesId);
    List<QuestionAnalysis> findQuestionAnalysis(Long homeworkPublishId, Long classesId,String bigNumber,String smallNumber,Boolean isCorrect);

    List<QuestionAnalysis> findListByStudHomeId(Long studentsHomeworkId);

    void deleteByStudHomeId(Long studentsHomeworkId);

    void deleteById(Long id);
}
