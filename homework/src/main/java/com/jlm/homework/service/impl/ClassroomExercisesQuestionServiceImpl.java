package com.jlm.homework.service.impl;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.ClassroomExercisesQuestionRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IQuestionBankService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassroomExercisesQuestionServiceImpl implements IClassroomExercisesQuestionService {
    @Resource
    private ClassroomExercisesQuestionRepository classroomExercisesQuestionRepository;
    @Autowired
    private IQuestionBankService questionBankService;
    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;
    @Override
    public void saveQuestionList(Long classroomExercisesId, List<ClassroomExercisesQuestion> questionList) {
        if(questionList==null||questionList.isEmpty()){
            return;
        }

        for(ClassroomExercisesQuestion question : questionList){
            question.setClassroomExercisesId(classroomExercisesId);
            if(question.getQuestionBankId()!=null){
                QuestionBank questionBank=questionBankService.getById(question.getQuestionBankId());
                if(questionBank!=null){
                    question.setQuestionType(questionBank.getQuestionType());
                    question.setDifficulty(questionBank.getDifficulty());
                    question.setOptions(questionBank.getOptions());
                    question.setParse(questionBank.getParse());
                    question.setSubject(questionBank.getSubject());
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
        List<ClassroomExercisesQuestion>  questionList= classroomExercisesQuestionRepository.findAll(Example.of(question));
        for(ClassroomExercisesQuestion exercisesQuestion : questionList){
            if(exercisesQuestion.getQuestionBankId()!=null){
                QuestionBank questionBank=questionBankService.getById(exercisesQuestion.getQuestionBankId());
                if(questionBank!=null){
                    exercisesQuestion.setQuestionType(questionBank.getQuestionType());
                    exercisesQuestion.setDifficulty(questionBank.getDifficulty());
                    exercisesQuestion.setOptions(questionBank.getOptions());
                    exercisesQuestion.setParse(questionBank.getParse());
                    exercisesQuestion.setSubject(questionBank.getSubject());
                }
            }

        }
        return questionList;
    }

    @Override
    public ClassroomExercisesQuestion save(Long classroomExercisesId, ClassroomExercisesQuestion question) {
        question.setClassroomExercisesId(classroomExercisesId);
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
                    condition2 = criteriaBuilder.like(root.get("classIds"),"%"+classId+"%");
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
            }else{


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
    public ExerciseTypeAnalyse exerciseTypeAnalyse(Long classId, String startDate, String endDate) {
        ExerciseTypeAnalyse exerciseTypeAnalyse =  new ExerciseTypeAnalyse();
        Specification<ClassroomExercisesQuestion> specification = new Specification<ClassroomExercisesQuestion>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesQuestion> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

                Predicate condition2 = null;
                if(classId!=null){
                    condition2 = criteriaBuilder.like(root.get("classIds"),"%"+classId+"%");
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
        Map<String,Integer> exerciseTypeMap = new HashMap<>();
        Map<String,Integer> dayExerciseTypeMap = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String,String> questionIdMap = new HashMap<>();
        for(ClassroomExercisesQuestion question:questionList){
            if (exerciseTypeMap.containsKey(question.getQuestionType())){
                exerciseTypeMap.put(question.getQuestionType(),exerciseTypeMap.get(question.getQuestionType())+1);
            }else {
                exerciseTypeMap.put(question.getQuestionType(),1);
            }
            if (questionIdMap.containsKey(question.getQuestionType())){
                questionIdMap.put(question.getQuestionType(),questionIdMap.get(question.getQuestionType())+","+question.getId());
            }else{
                questionIdMap.put(question.getQuestionType(),""+question.getId());
            }
            String day = sdf.format(question.getCreateTime());
            String key = question.getQuestionType()+":"+day;
            if (dayExerciseTypeMap.containsKey(key)){
                dayExerciseTypeMap.put(key,dayExerciseTypeMap.get(key)+1);
            }else {
                dayExerciseTypeMap.put(key,1);
            }
        }
        exerciseTypeAnalyse.setExerciseTypeMap(exerciseTypeMap);
        List<DayExerciseTypeNum> dayExerciseTypeNumList = new ArrayList<>();
        for(String key:dayExerciseTypeMap.keySet()){
            String exerciseType = key.split(":")[0];
            String day = key.split(":")[1];
            DayExerciseTypeNum dayExerciseTypeNum = new DayExerciseTypeNum();
            dayExerciseTypeNum.setDay(day);
            dayExerciseTypeNum.setExerciseType(exerciseType);
            dayExerciseTypeNum.setNum(dayExerciseTypeMap.get(key));
            dayExerciseTypeNumList.add(dayExerciseTypeNum);
        }
        exerciseTypeAnalyse.setDayExerciseTypeNumList(dayExerciseTypeNumList);
        List<ClassroomExercisesStudentAnswer> studentAnswerList = findByClassAndDate(classId, startDate, endDate);
        Map<String,Integer> studentAnswerMap = new HashMap<>();
        for (ClassroomExercisesStudentAnswer studentAnswer : studentAnswerList) {
            String key = studentAnswer.getStudentId()+":"+studentAnswer.getExerciseQuestionId();
            if(studentAnswerMap.containsKey(key)) {

            }
        }
        Map<String,Integer> studentAnswerTypeMap = new HashMap<>();
        for (String type:questionIdMap.keySet()) {
            List<Long> typeQuestionIdList = Arrays.stream(questionIdMap.get(type).split(",")).map(Long::parseLong).collect(Collectors.toList());

        }
        return exerciseTypeAnalyse;
    }

    // 私有方法：按班级与时间范围查询学生课堂练习作答记录，避免对其它 Service 的依赖，打破循环依赖
    private List<ClassroomExercisesStudentAnswer> findByClassAndDate(Long classId, String startDate, String endDate) {
        Specification<ClassroomExercisesStudentAnswer> specification = new Specification<ClassroomExercisesStudentAnswer>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentAnswer> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition2;
                if (classId != null) {
                    condition2 = criteriaBuilder.equal(root.get("classId"), classId);
                } else {
                    condition2 = criteriaBuilder.conjunction();
                }
                Predicate condition3;
                if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition3 = criteriaBuilder.between(root.get("createTime"), startDate1, endDate1);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    condition3 = criteriaBuilder.conjunction();
                }
                query.where(condition2, condition3);
                return null;
            }
        };
        return classroomExercisesStudentAnswerRepository.findAll(specification);
    }
}
