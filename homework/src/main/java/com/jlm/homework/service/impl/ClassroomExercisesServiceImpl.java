package com.jlm.homework.service.impl;

import com.jlm.homework.dto.DayExerciseTypeNum;
import com.jlm.homework.dto.ExerciseTypeAnalyse;
import com.jlm.homework.dto.ExerciseTypeErrorRate;
import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IClassroomExercisesService;
import com.jlm.homework.service.IClassroomExercisesStudentAnswerService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassroomExercisesServiceImpl implements IClassroomExercisesService {
    @Resource
    private ClassroomExercisesRepository classroomExercisesRepository;
    @Autowired
    private IClassroomExercisesQuestionService classroomExercisesQuestionService;
    @Autowired
    private IClassroomExercisesStudentAnswerService  classroomExercisesStudentAnswerService;
    @Resource
    private ClassroomExercisesStudentRecordRepository classroomExercisesStudentRecordRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private StudentFeginClient studentFeginClient;
    @Override
    public Long create(ClassroomExercises classroomExercises) {
        List<ClassroomExercisesQuestion> questionList=classroomExercises.getQuestionList();
        if(classroomExercises.getSchoolId()==null){
            classroomExercises.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        classroomExercises =classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises.getId();
    }

    @Override
    public ClassroomExercises getById(Long id) {
        ClassroomExercises  classroomExercises = classroomExercisesRepository.getById(id);
        if(classroomExercises != null){
            List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.selectQuestionList(classroomExercises.getId());
            classroomExercises.setQuestionList(questionList);
        }
        return classroomExercises;
    }

    @Override
    public ClassroomExercises update(ClassroomExercises classroomExercises) {
        List<ClassroomExercisesQuestion> questionList=classroomExercises.getQuestionList();
        classroomExercises = classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises;
    }

    @Override
    public Page<ClassroomExercises> selectList(Integer pageNum, Integer pageSize, ClassroomExercises classroomExercises) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Page<ClassroomExercises> page= classroomExercisesRepository.findAll(Example.of(classroomExercises),pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        for(ClassroomExercises item:exercisesList){
            List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.selectQuestionList(item.getId());
            item.setQuestionList(questionList);
        }

        return page;
    }

    @Override
    public void deleteById(Long id) {
        classroomExercisesRepository.deleteById(id);
    }

    @Override
    public ClassroomExercises publish(ClassroomExercises classroomExercises) {
        classroomExercises.setPublishStatus(1);
        classroomExercises.setPublishTime(new Date());
        List<ClassroomExercisesQuestion> questionList =classroomExercises.getQuestionList();
        classroomExercises = classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises;
    }

    @Override
    public ExerciseTypeAnalyse exerciseTypeAnalyse(Long classId, String startDate, String endDate) {
        ExerciseTypeAnalyse exerciseTypeAnalyse =  new ExerciseTypeAnalyse();
        List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.findQuestionList(classId,startDate,endDate);
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
        List<ExerciseTypeErrorRate>  typeErrorRateList = new ArrayList<>();
        for (String type:questionIdMap.keySet()) {
            List<Long> typeQuestionIdList = Arrays.stream(questionIdMap.get(type).split(",")).map(Long::parseLong).collect(Collectors.toList());
            ExerciseTypeErrorRate  exerciseTypeErrorRate = new ExerciseTypeErrorRate();
            exerciseTypeErrorRate.setTitleNum(typeQuestionIdList.size());
            exerciseTypeErrorRate.setTitleType(type);
            List<ClassroomExercisesStudentAnswer> studentAnswerList=classroomExercisesStudentAnswerService.findByQuestionIdList(typeQuestionIdList);
            if(studentAnswerList.size()>0) {
                int rightNum = 0;
                int wrongNum = 0;
                for (ClassroomExercisesStudentAnswer studentAnswer : studentAnswerList) {
                    if (1 == studentAnswer.getRightFlag()) {
                        rightNum++;
                    } else if (0 == studentAnswer.getRightFlag()) {
                        wrongNum++;
                    }
                }
                exerciseTypeErrorRate.setRightNum(rightNum);
                Double errorRate = BigDecimal.valueOf(wrongNum).divide(BigDecimal.valueOf(studentAnswerList.size()), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                exerciseTypeErrorRate.setErrorRate(errorRate);
            }
            typeErrorRateList.add(exerciseTypeErrorRate);
        }
        exerciseTypeAnalyse.setTypeErrorRateList(typeErrorRateList);
        return exerciseTypeAnalyse;
    }

    @Override
    public void teacherStartAnswer(Long classroomExercisesId, Long classId ,Long schoolId) {
        ClassroomExercises classroomExercises = new ClassroomExercises();

        if(classroomExercisesId==0&&schoolId==null){//老师黑板现场出题
            schoolId = userService.getCurrentSchoolIdSafely();

        }else if(classroomExercisesId!=0&&schoolId==null){
            classroomExercises=classroomExercisesRepository.findById(classroomExercisesId).get();
            schoolId =  classroomExercises.getSchoolId();
        }

        if(!classroomExercises.getClassIds().contains(classId)) {
            throw new RuntimeException("此随堂练习和班级不对应，请检测！");
        }
        Date now = new Date();
        Result<Student> result = studentFeginClient.getStudentList(1,200,schoolId,null,classId,"0");
        if(result.getCode()!=200){
            throw new RuntimeException(result.getMsg());
        }
        List<Student> studentList=result.getRows();
        if(studentList.size()==0){
            throw new RuntimeException("该班级还没有学生呢，请检查！");
        }
        String className = null;
        if(classroomExercisesId == 0){
            className = userService.getCurrentUserInfo().getTeacherName();
        }else {
            className = classroomExercises.getClassNames().get(classroomExercises.getClassIds().indexOf(classId));
        }
        for (Student student:studentList){
            ClassroomExercisesStudentRecord record=new ClassroomExercisesStudentRecord();
            record.setStudentId(student.getStudentId());
            record.setStudentName(student.getStudentName());
            record.setStudentImage(student.getStudentImage());
            record.setClassId(classId);
            record.setClassName(className);
            record.setCreateTime(now);
            record.setClassroomExercisesId(classroomExercisesId);
            record.setStartTime(now);
            record.setStartFlag(1);
            classroomExercisesStudentRecordRepository.save(record);
        }
    }


}
