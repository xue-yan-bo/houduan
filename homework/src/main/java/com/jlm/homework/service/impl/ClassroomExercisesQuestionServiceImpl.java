package com.jlm.homework.service.impl;

import com.jlm.homework.dto.AverageDurationAnalyse;
import com.jlm.homework.dto.ClassroomExercisesData;
import com.jlm.homework.dto.DayTitleVolume;
import com.jlm.homework.dto.TitleVolumeAnalyse;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.ClassroomExercisesQuestionRepository;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IQuestionBankService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ClassroomExercisesQuestionServiceImpl implements IClassroomExercisesQuestionService {
    @Resource
    private ClassroomExercisesQuestionRepository classroomExercisesQuestionRepository;
    @Autowired
    private IQuestionBankService questionBankService;
    @Resource
    private ClassroomExercisesRepository classroomExercisesRepository;
    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;
    @Override
    public void saveQuestionList(Long classroomExercisesId, List<ClassroomExercisesQuestion> questionList) {
        if(questionList==null||questionList.isEmpty()){
            return;
        }
        ClassroomExercises classroomExercises =classroomExercisesRepository.findById(classroomExercisesId).get();
        for(ClassroomExercisesQuestion question : questionList){
            question.setClassroomExercisesId(classroomExercisesId);
            if(question.getClassIds()==null){
                question.setClassIds(classroomExercises.getClassIds());
                question.setClassNames(classroomExercises.getClassNames());
            }
            if(question.getQuestionBankId()!=null){
                QuestionBank questionBank=questionBankService.getById(question.getQuestionBankId());
                if(questionBank!=null){
                    question.setQuestionType(questionBank.getQuestionType());
                    question.setDifficulty(questionBank.getDifficulty());
                    question.setOptions(questionBank.getOptions());
                    question.setParse(questionBank.getParse());
                    question.setSubject(questionBank.getSubject());
                    if("math".equals(questionBank.getSubject())){
                        question.setSubject("数学");
                    }
                }
            }
            question.setCreateTime(new Date());
        }
        classroomExercisesQuestionRepository.saveAll(questionList);
    }

    @Override
    public List<ClassroomExercisesQuestion>  selectQuestionList(Long classroomExercisesId) {
        ClassroomExercisesQuestion question = new ClassroomExercisesQuestion();
        question.setClassroomExercisesId(classroomExercisesId);
        Sort sort = Sort.by(Sort.Direction.ASC, "titleNumber");
        List<ClassroomExercisesQuestion>  questionList= classroomExercisesQuestionRepository.findAll(Example.of(question),sort);
        for(ClassroomExercisesQuestion exercisesQuestion : questionList){
            if(exercisesQuestion.getQuestionBankId()!=null){
                QuestionBank questionBank=questionBankService.getById(exercisesQuestion.getQuestionBankId());
                if(questionBank!=null){
                    exercisesQuestion.setQuestionType(questionBank.getQuestionType());
                    exercisesQuestion.setDifficulty(questionBank.getDifficulty());
                    exercisesQuestion.setOptions(questionBank.getOptions());
                    exercisesQuestion.setParse(questionBank.getParse());
                    exercisesQuestion.setSubject(questionBank.getSubject());
                    if("math".equals(questionBank.getSubject())){
                        exercisesQuestion.setSubject("数学");
                    }
                }
            }

        }
        return questionList;
    }

    @Override
    public ClassroomExercisesQuestion save(Long classroomExercisesId, ClassroomExercisesQuestion question) {
        question.setClassroomExercisesId(classroomExercisesId);
        if(question.getCreateTime()==null){
            question.setCreateTime(new Date());
        }
        return classroomExercisesQuestionRepository.save(question);
    }

    @Override
    public void delete(Long id) {
        classroomExercisesQuestionRepository.deleteById(id);
    }

    @Override
    public ClassroomExercisesQuestion findById(Long id) {
        return classroomExercisesQuestionRepository.findById(id).get();
    }

    @Override
    public TitleVolumeAnalyse titleVolumeAnalyse(String subject, Long classId, String startDate, String endDate) {
        TitleVolumeAnalyse titleVolumeAnalyse =new TitleVolumeAnalyse();
        Specification<ClassroomExercisesQuestion> specification = new Specification<ClassroomExercisesQuestion>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesQuestion> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition1 = null;
                if(StringUtils.isNotEmpty(subject)){
                    condition1 = criteriaBuilder.equal(root.get("subject"),subject);
                }else {
                    condition1 = criteriaBuilder.conjunction();
                }
                Predicate condition2 = null;
                if(classId!=null){
                    condition2 = criteriaBuilder.like(root.get("classIds").as(String.class),"%"+classId+"%");
                }else {
                    condition2 = criteriaBuilder.conjunction();
                }
                Predicate condition3 = null;
                if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition3 = criteriaBuilder.between(root.get("createTime"),startDate1,endDate1);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }else {
                    condition3 = criteriaBuilder.conjunction();
                }
                query.where(condition1,condition2,condition3);
                return null;
            }
        };
        List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionRepository.findAll(specification);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String,Integer> dayTitleNumMap = new HashMap<>();
        Map<String,Integer> cdsTitleNumMap = new HashMap<>();
        Map<String,Integer> knowledgeMap = new HashMap<>();
        Map<Long,String> classMap = new HashMap<>();
        for(ClassroomExercisesQuestion exercisesQuestion : questionList){
            String day = sdf.format(exercisesQuestion.getCreateTime());
            if(dayTitleNumMap.containsKey(day)){
                dayTitleNumMap.put(day,dayTitleNumMap.get(day)+1);
            }else {
                dayTitleNumMap.put(day,1);
            }
            List<String> classNames = exercisesQuestion.getClassNames();
            List<Long> classIds = exercisesQuestion.getClassIds();
            if(classId!=null){
                classMap.put(classId,classNames.get(classIds.indexOf(classId)));
                String key = exercisesQuestion.getSubject()+":"+day+":"+classId;
                if(cdsTitleNumMap.containsKey(key)){
                    cdsTitleNumMap.put(key,cdsTitleNumMap.get(key)+1);
                }else {
                    cdsTitleNumMap.put(key,1);
                }
                String knowledgeKey = key+":"+exercisesQuestion.getKnowledgePoint();
                if(knowledgeMap.containsKey(knowledgeKey)){
                    knowledgeMap.put(knowledgeKey,knowledgeMap.get(knowledgeKey)+1);
                }else {
                    knowledgeMap.put(knowledgeKey,1);
                }
            }else if(classIds!=null&&classIds.size()>0){
                for(Long classId1 : classIds){
                    classMap.put(classId1,classNames.get(classIds.indexOf(classId1)));
                    String key = exercisesQuestion.getSubject()+":"+day+":"+classId1;
                    if(cdsTitleNumMap.containsKey(key)){
                        cdsTitleNumMap.put(key,cdsTitleNumMap.get(key)+1);
                    }else {
                        cdsTitleNumMap.put(key,1);
                    }
                    String knowledgeKey = key+":"+exercisesQuestion.getKnowledgePoint();
                    if(knowledgeMap.containsKey(knowledgeKey)){
                        knowledgeMap.put(knowledgeKey,knowledgeMap.get(knowledgeKey)+1);
                    }else {
                        knowledgeMap.put(knowledgeKey,1);
                    }
                }
            }

        }
        List<DayTitleVolume> dayTitleVolumeList = new ArrayList<>();
        for(String day:dayTitleNumMap.keySet()){
            DayTitleVolume  dayTitleVolume = new DayTitleVolume();
            dayTitleVolume.setDay(day);
            dayTitleVolume.setTitleNum(dayTitleNumMap.get(day));
            dayTitleVolumeList.add(dayTitleVolume);
        }
        titleVolumeAnalyse.setDayTitleVolumeList(dayTitleVolumeList);
        List<AverageDurationAnalyse> averageDurationAnalyseList = new ArrayList<>();
        for(String key:cdsTitleNumMap.keySet()){
            String subjectStr = key.split(":")[0];
            String day = key.split(":")[1];
            String classId2 = key.split(":")[2];

            AverageDurationAnalyse durationAnalyse = new AverageDurationAnalyse();
            durationAnalyse.setSubject(subjectStr);
            durationAnalyse.setDay(day);
            durationAnalyse.setClassId(Long.valueOf(classId2));
            durationAnalyse.setClassName(classMap.get(Long.valueOf(classId2)));
            durationAnalyse.setTitleNum(cdsTitleNumMap.get(key));
            Integer knowledgeNum=0;
            for(String knowledgeKey :knowledgeMap.keySet()){
                if(knowledgeKey.contains(key)){
                    knowledgeNum +=1;
                }
            }
            durationAnalyse.setKnowledgeNum(knowledgeNum);
            averageDurationAnalyseList.add(durationAnalyse);
        }
        titleVolumeAnalyse.setAverageDurationAnalyseList(averageDurationAnalyseList);
        return titleVolumeAnalyse;
    }

    @Override
    public List<ClassroomExercisesQuestion> findQuestionList(Long classId, String startDate, String endDate) {
        Specification<ClassroomExercisesQuestion> specification = new Specification<ClassroomExercisesQuestion>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesQuestion> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

                Predicate condition2 = null;
                if(classId!=null){
                    condition2 = criteriaBuilder.like(root.get("classIds").as(String.class),"%"+classId+"%");
                }else {
                    condition2 = criteriaBuilder.conjunction();
                }
                Predicate condition3 = null;
                if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition3 = criteriaBuilder.between(root.get("createTime"),startDate1,endDate1);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }else {
                    condition3 = criteriaBuilder.conjunction();
                }
                query.where(condition2,condition3);
                return null;
            }
        };
        List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionRepository.findAll(specification);
        return questionList;

    }

    @Override
    public List<ClassroomExercisesData> classroomExercisesData(Long classroomExercisesId, Long classId) {
        List<ClassroomExercisesData> exercisesDataList = new ArrayList<>();
        ClassroomExercisesQuestion quation = new ClassroomExercisesQuestion();
        quation.setClassroomExercisesId(classroomExercisesId);
        quation.setClassIds(Arrays.asList(classId));
        List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionRepository.findAll(Example.of(quation));
        for(ClassroomExercisesQuestion classroomExercisesQuestion:questionList){
            ClassroomExercisesData  classroomExercisesData = new ClassroomExercisesData();
            classroomExercisesData.setTitleNumber(classroomExercisesQuestion.getTitleNumber());
            classroomExercisesData.setAnswer(classroomExercisesQuestion.getAnswer());
            ClassroomExercisesStudentAnswer studentAnswer = new ClassroomExercisesStudentAnswer();
            studentAnswer.setClassId(classId);
            studentAnswer.setExerciseQuestionId(classroomExercisesQuestion.getId());
            studentAnswer.setTitleNumber(classroomExercisesQuestion.getTitleNumber());
            List<ClassroomExercisesStudentAnswer> studentAnswerList=classroomExercisesStudentAnswerRepository.findAll(Example.of(studentAnswer));
            Integer answerNum=0;
            Integer unAnswerNum=0;
            Integer rightNum=0;
            Integer errorNum=0;
            for(ClassroomExercisesStudentAnswer studentAnswer1:studentAnswerList){
                if(StringUtils.isNotEmpty(studentAnswer1.getStudentAnswer())){
                    answerNum++;
                    String answer = new String(Base64.getDecoder().decode(studentAnswer1.getAnswer()));
                    if(studentAnswer1.getStudentAnswer().equals(studentAnswer1.getAnswer())||
                            answer.equals(studentAnswer1.getStudentAnswer())){
                        rightNum++;
                    }else{
                        errorNum++;
                    }
                }else{
                    unAnswerNum++;
                }
            }
            classroomExercisesData.setAnswerNum(answerNum);
            classroomExercisesData.setUnAnswerNum(unAnswerNum);
            classroomExercisesData.setRightNum(rightNum);
            classroomExercisesData.setErrorNum(errorNum);
            exercisesDataList.add(classroomExercisesData);
        }
        return exercisesDataList;
    }

    @Override
    public void saveQuestion(ClassroomExercisesQuestion cpQuestion) {
        if(cpQuestion.getCreateTime()==null){
            cpQuestion.setCreateTime(new Date());
        }
        classroomExercisesQuestionRepository.save(cpQuestion);
    }

    @Override
    public List<ClassroomExercisesQuestion> selectQuestionListByExercisesIds(List<Long> classroomExercisesIds) {
        if (classroomExercisesIds == null || classroomExercisesIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建查询条件
        List<ClassroomExercisesQuestion> allQuestions = new ArrayList<>();
        for (Long exercisesId : classroomExercisesIds) {
            ClassroomExercisesQuestion question = new ClassroomExercisesQuestion();
            question.setClassroomExercisesId(exercisesId);
            Sort sort = Sort.by(Sort.Direction.ASC, "titleNumber");
            List<ClassroomExercisesQuestion> questionList = classroomExercisesQuestionRepository.findAll(Example.of(question), sort);
            allQuestions.addAll(questionList);
        }
        
        // 填充问题银行信息
        for (ClassroomExercisesQuestion exercisesQuestion : allQuestions) {
            if (exercisesQuestion.getQuestionBankId() != null) {
                QuestionBank questionBank = questionBankService.getById(exercisesQuestion.getQuestionBankId());
                if (questionBank != null) {
                    exercisesQuestion.setQuestionType(questionBank.getQuestionType());
                    exercisesQuestion.setDifficulty(questionBank.getDifficulty());
                    exercisesQuestion.setOptions(questionBank.getOptions());
                    exercisesQuestion.setParse(questionBank.getParse());
                    exercisesQuestion.setSubject(questionBank.getSubject());
                    if ("math".equals(questionBank.getSubject())) {
                        exercisesQuestion.setSubject("数学");
                    }
                }
            }
        }
        
        return allQuestions;
    }


}
