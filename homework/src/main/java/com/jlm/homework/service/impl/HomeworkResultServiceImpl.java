package com.jlm.homework.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.jlm.homework.dto.HomeworkAIBigDto;
import com.jlm.homework.dto.HomeworkAISmallDto;
import com.jlm.homework.entity.HomeworkPublishQuestion;
import com.jlm.homework.entity.QuestionAnalysis;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.mq.CorrectionStatus;
import com.jlm.homework.entity.mq.HomeworkCorrectionResult;
import com.jlm.homework.entity.mq.QuestionCorrectionResult;
import com.jlm.homework.repository.HomeworkPublishQuestionRepository;
import com.jlm.homework.service.IHomeworkResultService;
import com.jlm.homework.service.IQuestionAnalysisService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HomeworkResultServiceImpl implements IHomeworkResultService {

    @Autowired
    private IQuestionAnalysisService questionAnalysisService;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Resource
    private HomeworkPublishQuestionRepository homeworkPublishQuestionRepository;
    @Override
    public void processHomeworkResult(HomeworkCorrectionResult message) {
        log.info("处理批改结果开始 - homeworkId: {}, studentId: {}",message.getHomeworkId(),message.getStudentId());
        // TODO: 实现具体的作业结果处理逻辑
        Long studentHomeworkId = Long.parseLong(message.getHomeworkId());

        if(CorrectionStatus.SUCCESS.equals(message.getStatus())&&message.getQuestions()!=null) {
            StudentsHomeworkNew studentsHomework =studentsHomeworkNewService.getById(studentHomeworkId);

            List<HomeworkAIBigDto> bigDtoList = new ArrayList<>();
            Map<String,HomeworkAIBigDto>  bigMap = new HashMap<>();
            Map<String,Boolean>  map = new HashMap<>();
            for(QuestionCorrectionResult questionResult:message.getQuestions()){
                HomeworkPublishQuestion search = new HomeworkPublishQuestion();
                search.setSmallNumber(questionResult.getQuestionNumber());
                search.setQuestionType(questionResult.getQuestionType());
                HomeworkPublishQuestion publishQuestion=homeworkPublishQuestionRepository.findOne(Example.of(search)).orElse(null);
                QuestionAnalysis questionAnalysis = new QuestionAnalysis();
                BeanUtils.copyProperties(publishQuestion,questionAnalysis);
                questionAnalysis.setId(null);
                questionAnalysis.setSchoolId(studentsHomework.getSchoolId());
                questionAnalysis.setStudentsHomeworkId(studentHomeworkId);
                questionAnalysis.setStudentId(studentsHomework.getStudentId());
                questionAnalysis.setStudentName(studentsHomework.getStudentName());
                questionAnalysis.setGrade(studentsHomework.getGrade());
                questionAnalysis.setClassesId(studentsHomework.getClassesId());
                questionAnalysis.setStudentAnswer(questionResult.getStudentAnswer());
                questionAnalysis.setIsCorrect(questionResult.getIsCorrect());
                String key = questionAnalysis.getBigNumber()+":"+questionAnalysis.getQuestionType()+":"+questionAnalysis.getSmallNumber();
                map.put(key,questionResult.getIsCorrect());
                String bigKey  = questionAnalysis.getBigNumber()+":"+questionAnalysis.getQuestionType();
                HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
                smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
                if(questionResult==null){
                    smallDto.setCorrectFlag("未作答");
                }else if(questionResult.getIsCorrect()){
                    smallDto.setCorrectFlag("正确");
                }else{
                    smallDto.setCorrectFlag("错误");
                }

                if(bigMap.containsKey(bigKey)){
                    HomeworkAIBigDto bigDto= bigMap.get(bigKey);
                    List<HomeworkAISmallDto> smallDtoList = bigDto.getSmallDtoList();
                    smallDtoList.add(smallDto);
                    bigMap.put(bigKey,bigDto);
                }else{
                    HomeworkAIBigDto bigDto = new HomeworkAIBigDto();
                    bigDto.setBigNumber(questionAnalysis.getBigNumber());
                    bigDto.setQuestionType(questionAnalysis.getQuestionType());
                    List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
                    smallDtoList.add(smallDto);
                    bigDto.setSmallDtoList(smallDtoList);
                    bigMap.put(bigKey,bigDto);
                }
                questionAnalysis.setObtainedScore(questionResult.getScore());
                if("1".equals(message.getType())) {
                    questionAnalysisService.save(questionAnalysis);
                }

            }
            if("1".equals(message.getType())) {
                studentsHomework.setAiAudit(JSONArray.toJSONString(bigDtoList));
                studentsHomework.setScore(message.getTotalScore());
            } else if ("2".equals(message.getType())){
                studentsHomework.setAiAudit2(JSONArray.toJSONString(bigDtoList));
            }
            studentsHomeworkNewService.update(studentsHomework);
        }
    }
}
