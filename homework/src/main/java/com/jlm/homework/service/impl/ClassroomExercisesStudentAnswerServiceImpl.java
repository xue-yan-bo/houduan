package com.jlm.homework.service.impl;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.entity.Student;
import com.jlm.homework.repository.ClassroomExercisesQuestionRepository;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.service.IClassroomExercisesStudentAnswerService;
import com.jlm.homework.service.IWrongTitleBookService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ClassroomExercisesStudentAnswerServiceImpl implements IClassroomExercisesStudentAnswerService {
    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;

    @Autowired
    private ClassroomExercisesRepository classroomExercisesRepository;
    @Autowired
    private ClassroomExercisesQuestionRepository classroomExercisesQuestionRepository;
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;
    @Autowired
    private StudentFeignClient studentFeignClient;


    @Override
    public ClassroomExercisesStudentAnswer save(ClassroomExercisesStudentAnswer studentAnswer) {
        if(StringUtils.isNotEmpty(studentAnswer.getStudentAnswer())){
            if(studentAnswer.getStudentAnswer().equals(studentAnswer.getAnswer())){
                studentAnswer.setRightFlag(1);
            }else{
                ClassroomExercisesQuestion question = classroomExercisesQuestionRepository.findById(studentAnswer.getExerciseQuestionId()).get();
                WrongTitleBook wrongTitleBook = new WrongTitleBook();
                wrongTitleBook.setSource("随堂检测");
                wrongTitleBook.setExercisesRecordId(studentAnswer.getClassroomExercisesId());
                wrongTitleBook.setQuestionId(question.getId());
                wrongTitleBook.setStudentId(Long.parseLong(studentAnswer.getStudentId()));
                wrongTitleBook.setStudentName(studentAnswer.getStudentName());
                wrongTitleBook.setTitleBigNo(studentAnswer.getTitleNumber()+"");
                wrongTitleBook.setTitleContext(studentAnswer.getQuestionContent());
                wrongTitleBook.setTitleAnswer(studentAnswer.getAnswer());
                wrongTitleBook.setStudentAnswer(studentAnswer.getStudentAnswer());
                wrongTitleBook.setParse(question.getParse());
                wrongTitleBook.setCreateTime(new Date());
                wrongTitleBook.setClassId(studentAnswer.getClassId());
                wrongTitleBook.setClassName(studentAnswer.getClassName());
                wrongTitleBookService.save(wrongTitleBook);
            }

        }
        return classroomExercisesStudentAnswerRepository.save(studentAnswer);
    }

    @Override
    public ClassroomExercisesStudentAnswer findById(Long id) {
        return classroomExercisesStudentAnswerRepository.findById(id).get();
    }

    @Override
    public List<ClassroomExercisesStudentAnswer> findByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentAnswer answer =new ClassroomExercisesStudentAnswer();
        answer.setClassroomExercisesId(classroomExercisesId);
        return classroomExercisesStudentAnswerRepository.findAll(Example.of(answer));
    }

    @Override
    public ClassroomExercisesStudentStatistics statisticsByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentStatistics statistics = new ClassroomExercisesStudentStatistics();
        ClassroomExercises classroomExercises=classroomExercisesRepository.getById(classroomExercisesId);
        List<ClassroomExercisesStudentAnswer> studentAnswerList = this.findByClassroomExercisesId(classroomExercisesId);
        Integer studentTotal = 0;
        List<Long> classIds=classroomExercises.getClassIds();
        for(Long classId:classIds){
            Result<Student> result = studentFeignClient.getStudentList(1,150,classroomExercises.getSchoolId(),null,classId,"0");
            studentTotal = studentTotal+result.getRows().size();
        }
        Map<Integer,Integer> answerNumMap = new HashMap<>();
        Map<Integer,Integer> answerRightMap = new HashMap<>();
        Map<Integer,Integer> answerWrongMap = new HashMap<>();
        Map<String,Integer> variousAnswersNum = new HashMap<>();
        Map<Integer,String>  correctAnswerMap = new HashMap<>();
        List<StudentAnswerDto>  studentAnswerDtoList = new ArrayList<>();
        for (ClassroomExercisesStudentAnswer studentAnswer : studentAnswerList) {
            if(answerNumMap.containsKey(studentAnswer.getTitleNumber())) {//答题数量
                answerNumMap.put(studentAnswer.getTitleNumber(), answerNumMap.get(studentAnswer.getTitleNumber()) + 1);
            }else{
                answerNumMap.put(studentAnswer.getTitleNumber(), 1);
            }
            if(studentAnswer.getRightFlag()!=null&&studentAnswer.getRightFlag()==1) {//正确数量
                if (answerRightMap.containsKey(studentAnswer.getTitleNumber())) {
                    answerRightMap.put(studentAnswer.getTitleNumber(), answerRightMap.get(studentAnswer.getTitleNumber()) + 1);
                } else {
                    answerRightMap.put(studentAnswer.getTitleNumber(), 1);
                }
            }else if(studentAnswer.getRightFlag()!=null&&studentAnswer.getRightFlag()==0){//错误数量
                if (answerWrongMap.containsKey(studentAnswer.getTitleNumber())) {
                    answerWrongMap.put(studentAnswer.getTitleNumber(), answerWrongMap.get(studentAnswer.getTitleNumber()) + 1);
                } else {
                    answerWrongMap.put(studentAnswer.getTitleNumber(), 1);
                }
            }else {
                if (answerWrongMap.containsKey(studentAnswer.getTitleNumber())) {
                    answerWrongMap.put(studentAnswer.getTitleNumber(), answerWrongMap.get(studentAnswer.getTitleNumber()) + 1);
                } else {
                    answerWrongMap.put(studentAnswer.getTitleNumber(), 1);
                }
            }
            String key = studentAnswer.getTitleNumber() +":"+studentAnswer.getStudentAnswer();
            if(variousAnswersNum.containsKey(key)) {//答题数量
                variousAnswersNum.put(key, variousAnswersNum.get(key) + 1);
            }else{
                variousAnswersNum.put(key, 1);
            }
            correctAnswerMap.put(studentAnswer.getTitleNumber(),studentAnswer.getAnswer());
        }
        statistics.setQuestionTotal(answerNumMap.size());
        Long totalAnwers = 0l;
        Long totalRightAnswers = 0l;
        Integer maxWrongNumber = 1;
        Integer maxWrongAnswers = 0;
        Map<Integer,Double> answerOverviewMap = new HashMap<>();
        for(Integer key : answerNumMap.keySet()) {
            totalAnwers =  totalAnwers + answerNumMap.get(key);
            if(answerRightMap.get(key)!=null) {
                totalRightAnswers = totalRightAnswers + answerRightMap.get(key);
            }
            if(maxWrongAnswers<answerNumMap.get(key)) {
                maxWrongNumber = key;
                maxWrongAnswers =  answerNumMap.get(key);
            }
            Double answerOverview = 0.0;
            if(answerNumMap.get(key)!=null&&studentTotal!=null&&studentTotal!=0) {
                answerOverview = BigDecimal.valueOf(answerNumMap.get(key)).divide(BigDecimal.valueOf(studentTotal), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            answerOverviewMap.put(key,answerOverview);
            StudentAnswerDto studentAnswerDto = new StudentAnswerDto();
            studentAnswerDto.setTitleNumber(key);
            studentAnswerDto.setAnswerStudentNumber(answerNumMap.get(key));
            if(answerNumMap.get(key)!=null) {
                studentAnswerDto.setUnanswerStudentNumber(studentTotal - answerNumMap.get(key));
            }
            studentAnswerDto.setRightNumber(answerRightMap.get(key)==null?0:answerRightMap.get(key));
            studentAnswerDto.setWrongNumber(answerWrongMap.get(key)==null?0:answerWrongMap.get(key));
            studentAnswerDto.setCorrectAnswer(correctAnswerMap.get(key));
            Map<String,Double> variousAnswersProp = new HashMap<>();

            for(String ansersKey:variousAnswersNum.keySet()){
                String titleN = ansersKey.split(":")[0];
                String answerN = ansersKey.split(":")[1];
                if("null".equals(titleN)) {
                    continue;
                }
                Integer titleNumer =  Integer.parseInt(titleN);
                if(titleNumer!=null&&titleNumer.compareTo(key)==0){
                    Integer answerNum=variousAnswersNum.get(ansersKey);
                    if(answerNum!=null&&answerNumMap.get(key)!=null&&answerNumMap.get(key)!=0) {
                        Double answerRate = new BigDecimal(answerNum).divide(new BigDecimal(answerNumMap.get(key)), 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(BigDecimal.valueOf(100l)).doubleValue();
                        variousAnswersProp.put(answerN, answerRate);
                    }
                }
            }
            studentAnswerDto.setVariousAnswersProp(variousAnswersProp);
            studentAnswerDtoList.add(studentAnswerDto);
        }
        if(totalRightAnswers!=null&&totalAnwers!=null&&totalAnwers!=0) {
            Double totalAccuracy = BigDecimal.valueOf(totalRightAnswers).divide(BigDecimal.valueOf(totalAnwers), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            statistics.setTotalAccuracy(totalAccuracy);
        }
        statistics.setAnswerOverviewMap(answerOverviewMap);
        statistics.setMaxWrongRateQuestion(maxWrongNumber);
        statistics.setStudentAnswerDtoList(studentAnswerDtoList);
        return statistics;
    }

    @Override
    public KnowledgePointAnalysis getKnowledgePointAnalysis(Integer pageNum, Integer pageSize,String subject, Long classId, String startDate, String endDate) {
        KnowledgePointAnalysis knowledgePointAnalysis =new KnowledgePointAnalysis();
        Specification<ClassroomExercisesStudentAnswer> specification = new Specification<ClassroomExercisesStudentAnswer>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentAnswer> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition1 = null;
                if(StringUtils.isNotEmpty(subject)){
                    condition1 = criteriaBuilder.equal(root.get("subject"),subject);
                }else {
                    condition1 = criteriaBuilder.conjunction();
                }
                Predicate condition2 = null;
                if(classId!=null){
                    condition2 = criteriaBuilder.equal(root.get("classId"),classId);
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

        List<ClassroomExercisesStudentAnswer> studentAnswerList = classroomExercisesStudentAnswerRepository.findAll(specification);
        Map<String,Integer> studentKnowledgeNum = new HashMap<>();
        Map<String,Integer> studentKnowledgeRightNum = new HashMap<>();
        Map<String,Integer> knowledgetitleMap = new HashMap<>();
        Map<String,String> studentNameMap = new HashMap<>();
        Map<String,String> classMap = new HashMap<>();
        Map<String,Integer> knowledgeRightNumMap = new HashMap<>();
        for(ClassroomExercisesStudentAnswer studentAnswer:studentAnswerList){
            String key = studentAnswer.getStudentId()+":"+studentAnswer.getKnowledgePoint()+":"+studentAnswer.getSubject();
            if(studentKnowledgeNum.containsKey(key)){
                studentKnowledgeNum.put(key,studentKnowledgeNum.get(key)+1);
            }else  {
                studentKnowledgeNum.put(key,1);
            }
            String knowledgePoint = studentAnswer.getKnowledgePoint();
            if(knowledgetitleMap.containsKey(knowledgePoint)){
                knowledgetitleMap.put(knowledgePoint,knowledgetitleMap.get(knowledgePoint)+1);
            }else{
                knowledgetitleMap.put(knowledgePoint,1);
            }
            if(studentAnswer.getRightFlag()!=null&&studentAnswer.getRightFlag()==1){
                if(studentKnowledgeRightNum.containsKey(key)){
                    studentKnowledgeRightNum.put(key,studentKnowledgeRightNum.get(key)+1);
                }else {
                    studentKnowledgeRightNum.put(key,1);
                }
                if(knowledgeRightNumMap.containsKey(knowledgePoint)){
                    knowledgeRightNumMap.put(knowledgePoint,knowledgeRightNumMap.get(knowledgePoint)+1);
                }else {
                    knowledgeRightNumMap.put(knowledgePoint,1);
                }
            }
            studentNameMap.put(studentAnswer.getStudentId(),studentAnswer.getStudentName());
            classMap.put(studentAnswer.getStudentId(),studentAnswer.getClassId()+":"+studentAnswer.getClassName());
        }
        List<StudentKnowledgePointAnalysis> studentKnowledgePointAnalysisList = new ArrayList<>();
        Map<String,Integer> knowledgeNumMap = new HashMap<>();

        Map<String,Integer> knowledgeTotalMap = new HashMap<>();
        for(String key:studentKnowledgeNum.keySet()){
            StudentKnowledgePointAnalysis  studentKnowledgePointAnalysis =new StudentKnowledgePointAnalysis();
            String studentId = key.split(":")[0];
            String knowledgePoint = key.split(":")[1];
            String subjectName = key.split(":")[2];
            if(StringUtils.isEmpty(knowledgePoint)&&StringUtils.isEmpty(studentId)){
                continue;
            }
            studentKnowledgePointAnalysis.setKnowledgePoint(knowledgePoint);
            studentKnowledgePointAnalysis.setSubject(subjectName);
            studentKnowledgePointAnalysis.setStudentId(studentId);
            if(knowledgeTotalMap.containsKey(knowledgePoint)){
                knowledgeTotalMap.put(knowledgePoint,knowledgeTotalMap.get(knowledgePoint)+1);
            }else {
                knowledgeTotalMap.put(knowledgePoint,1);
            }
            Double masteryRate = 0.0;
            if(studentKnowledgeRightNum.get(key)!=null&&studentKnowledgeNum.get(key)!=null&&studentKnowledgeNum.get(key)!=0) {
                masteryRate = BigDecimal.valueOf(studentKnowledgeRightNum.get(key))
                        .divide(BigDecimal.valueOf(studentKnowledgeNum.get(key)), 6, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            if(Double.valueOf(70).equals(masteryRate)){
                if(knowledgeNumMap.containsKey(knowledgePoint)){
                    knowledgeNumMap.put(knowledgePoint,knowledgeNumMap.get(knowledgePoint)+1);
                }else {
                    knowledgeNumMap.put(knowledgePoint,1);
                }
            }

            studentKnowledgePointAnalysis.setMasteryRate(masteryRate);
            studentKnowledgePointAnalysis.setStudentName(studentNameMap.get(studentId));
            String classInfo=classMap.get(studentId);
            if(StringUtils.isNotEmpty(classInfo)&&classInfo.contains(":")) {
                studentKnowledgePointAnalysis.setClassId(Long.valueOf(classInfo.split(":")[0]));
                studentKnowledgePointAnalysis.setClassName(classInfo.split(":")[1]);
            }
            studentKnowledgePointAnalysisList.add(studentKnowledgePointAnalysis);
        }
        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNum);
        int end = pageNum*pageSize > studentKnowledgePointAnalysisList.size()?studentKnowledgePointAnalysisList.size():pageNum*pageSize;
        List<StudentKnowledgePointAnalysis> contect = studentKnowledgePointAnalysisList.subList((pageNum-1)*pageSize,end);
        Page<StudentKnowledgePointAnalysis> page = new PageImpl<>(contect,pageable,studentKnowledgePointAnalysisList.size());
        knowledgePointAnalysis.setStudentKnowledgePointAnalysisList(page);
        List<KnowledgePointWholeAnalysis> wholeAnalysisList = new ArrayList<>();
        for(String key2:knowledgeTotalMap.keySet()){
            KnowledgePointWholeAnalysis  knowledgePointWholeAnalysis = new KnowledgePointWholeAnalysis();
            knowledgePointWholeAnalysis.setKnowledgePoint(key2);
            knowledgePointWholeAnalysis.setMasterQuantity(knowledgeNumMap.get(key2)==null?0:knowledgeNumMap.get(key2));
            Double masteryRate = 0.0;
            if(knowledgeRightNumMap.get(key2)!=null&&knowledgeRightNumMap.get(key2)!=null&&knowledgetitleMap.get(key2)!=0){
                masteryRate = BigDecimal.valueOf(knowledgeRightNumMap.get(key2))
                        .divide(BigDecimal.valueOf(knowledgetitleMap.get(key2)),6,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            knowledgePointWholeAnalysis.setMasteryRate(masteryRate);
            wholeAnalysisList.add(knowledgePointWholeAnalysis);
        }
        knowledgePointAnalysis.setWholeAnalysisList(wholeAnalysisList);
        return knowledgePointAnalysis;
    }

    @Override
    public List<ClassroomExercisesStudentAnswer> findByClassAndDate(Long classId, String startDate, String endDate) {
        Specification<ClassroomExercisesStudentAnswer> specification = new Specification<ClassroomExercisesStudentAnswer>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentAnswer> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition2 = null;
                if(classId!=null){
                    condition2 = criteriaBuilder.equal(root.get("classId"),classId);
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
        return classroomExercisesStudentAnswerRepository.findAll(specification);
    }

    @Override
    public List<ClassroomExercisesStudentAnswer> findByQuestionIdList(List<Long> typeQuestionIdList) {
        Specification<ClassroomExercisesStudentAnswer> specification = new Specification<ClassroomExercisesStudentAnswer>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentAnswer> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                list.add(criteriaBuilder.in(root.get("exerciseQuestionId")).value(typeQuestionIdList));
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return classroomExercisesStudentAnswerRepository.findAll(specification);
    }
}
