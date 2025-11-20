package com.jlm.homework.service.impl;

import com.jlm.homework.dto.AIQuestionAnalysisDto;
import com.jlm.homework.entity.QuestionAnalysis;
import com.jlm.homework.repository.QuestionAnalysisRepository;
import com.jlm.homework.service.IQuestionAnalysisService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionAnalysisServiceImpl implements IQuestionAnalysisService {
    @Resource
    private QuestionAnalysisRepository questionAnalysisRepository;

    @Override
    public void save(QuestionAnalysis questionAnalysis) {
        questionAnalysisRepository.save(questionAnalysis);
    }

    @Override
    public List<AIQuestionAnalysisDto> findAIQuestionStatistics(Long homeworkPublishId, Long classesId) {
        List<AIQuestionAnalysisDto> analysisDtoList = new ArrayList<>();
        QuestionAnalysis search = new QuestionAnalysis();
        search.setHomeworkPublishId(homeworkPublishId);
        search.setClassesId(classesId);
        Sort sort = Sort.by(Sort.Direction.ASC, "bigNumber","smallNumber");
        List<QuestionAnalysis> questionAnalysisList= questionAnalysisRepository.findAll(Example.of(search),sort);
        Map<String,Integer> map = new HashMap<>();
        for(QuestionAnalysis questionAnalysis:questionAnalysisList){
            String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getSmallNumber()+":"+questionAnalysis.getIsCorrect();
            if(map.containsKey(key)){
                map.put(key,map.get(key)+1);
            }else {
                map.put(key,1);
            }
        }
        for(String key:map.keySet()){
            String bigNumber = key.split(":")[0];
            String smallNumber = key.split(":")[1];
            String isCorrect = key.split(":")[2];

            AIQuestionAnalysisDto  aiQuestionAnalysisDto = new AIQuestionAnalysisDto();
            aiQuestionAnalysisDto.setBigNumber(bigNumber);
            aiQuestionAnalysisDto.setSmallNumber(smallNumber);
            if(analysisDtoList.contains(aiQuestionAnalysisDto)){
                aiQuestionAnalysisDto=analysisDtoList.get(analysisDtoList.indexOf(aiQuestionAnalysisDto));
            }
            if(isCorrect.equals("true")){
                aiQuestionAnalysisDto.setCorrectNum(map.get(key));
            }
            if(isCorrect.equals("false")){
                aiQuestionAnalysisDto.setErrorNum(map.get(key));
            }
            if(!analysisDtoList.contains(aiQuestionAnalysisDto)) {
                analysisDtoList.add(aiQuestionAnalysisDto);
            }
        }
        return analysisDtoList;
    }

    @Override
    public List<QuestionAnalysis> findQuestionAnalysis(Long homeworkPublishId, Long classesId,String bigNumber,String smallNumber, Boolean isCorrect) {
        QuestionAnalysis search = new QuestionAnalysis();
        search.setHomeworkPublishId(homeworkPublishId);
        search.setClassesId(classesId);
        if(StringUtils.isNotEmpty(bigNumber)){
            search.setBigNumber(bigNumber);
        }
        if(StringUtils.isNotEmpty(smallNumber)){
            search.setSmallNumber(smallNumber);
        }
        search.setIsCorrect(isCorrect);
        return questionAnalysisRepository.findAll(Example.of(search));
    }
}
