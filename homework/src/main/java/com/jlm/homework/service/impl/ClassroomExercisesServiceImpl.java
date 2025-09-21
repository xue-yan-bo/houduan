package com.jlm.homework.service.impl;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.service.*;
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
    private IUserService userService;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Override
    public Long create(ClassroomExercises classroomExercises) {
        List<ClassroomExercisesQuestion> questionList=classroomExercises.getQuestionList();
        if(classroomExercises.getSchoolId()==null){
            classroomExercises.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        classroomExercises.setExercisesType(1);//随堂检测
        classroomExercises.setCreateTime(new Date());
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
        classroomExercises.setExercisesType(1);
        if(classroomExercises.getCreateTime()==null){
            classroomExercises.setCreateTime(new Date());
        }
        classroomExercises.setPublishTime(new Date());
        if(classroomExercises.getSchoolId()==null||classroomExercises.getSchoolId()==0){
            classroomExercises.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
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
    public Long teacherStartAnswer(Long classroomExercisesId, Long classId ,Long schoolId,Integer exercisesType) {
        ClassroomExercises classroomExercises = new ClassroomExercises();

        if(classroomExercisesId==0&&schoolId==null){//老师黑板现场出题
            schoolId = userService.getCurrentSchoolIdSafely();

        }else if(classroomExercisesId!=0){
            classroomExercises=classroomExercisesRepository.findById(classroomExercisesId).get();
            if(schoolId==null||schoolId==0) {
                schoolId = classroomExercises.getSchoolId();
            }
        }

        Date now = new Date();
        Result<Student> result = studentFeignClient.getStudentList(1,200,schoolId,null,classId,"0");
        if(result.getCode()!=200){
            throw new RuntimeException(result.getMsg());
        }
        List<Student> studentList=result.getRows();
        if(studentList.size()==0){
            throw new RuntimeException("该班级还没有学生呢，请检查！");
        }
        String className = null;
        if(classroomExercisesId==null||classroomExercisesId == 0){
            CurrentUserInfo userInfo=userService.getCurrentUserInfo();
            String teacherName ="";
            if(userInfo!=null){
                teacherName = userService.getCurrentUserInfo().getTeacherName();
            }


            classroomExercises.setClassIds(Arrays.asList(classId));
            classroomExercises.setExercisesType(exercisesType);
            classroomExercises.setSchoolId(schoolId);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            classroomExercises.setDeleteFlag(0);
            if(2==exercisesType){
                classroomExercises.setHomeworkName(sdf.format(new Date())+"堂课互动");
            }else if(3==exercisesType){
                classroomExercises.setHomeworkName(sdf.format(new Date())+"纸笔直播");
            }

            classroomExercises.setCreateTime(new Date());
            classroomExercises =classroomExercisesRepository.save(classroomExercises);
            classroomExercisesId = classroomExercises.getId();

        }else{
            ClassroomExercisesStudentRecord studentRecord=new ClassroomExercisesStudentRecord();
            studentRecord.setClassId(classId);
            studentRecord.setClassroomExercisesId(classroomExercisesId);
            List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(studentRecord));
            if(recordList!=null&&recordList.size()>0){
                return classroomExercisesId;
            }
        }
        if(studentList!=null&&studentList.size()>0){
            Student student = studentList.get(0);
            if(classroomExercises.getClassNames()==null||classroomExercises.getClassNames().size()==0){
                classroomExercises.setClassNames(Arrays.asList(student.getClassesName()));
                classroomExercises.setGradeId(student.getGradeId());
                classroomExercises.setGradeName(student.getGradeName());
                classroomExercisesRepository.save(classroomExercises);
            }
        }

        for (Student student:studentList){
            ClassroomExercisesStudentRecord record=new ClassroomExercisesStudentRecord();
            record.setStudentId(student.getStudentId());
            record.setStudentName(student.getStudentName());
            record.setStudentImage(student.getStudentImage());
            record.setClassId(classId);
            record.setClassName(student.getClassesName());
            record.setCreateTime(now);
            record.setClassroomExercisesId(classroomExercisesId);
            record.setStartTime(now);
            record.setStartFlag(1);
            classroomExercisesStudentRecordRepository.save(record);

        }
        return classroomExercisesId;
    }

    @Override
    public TeacherClassroomData getTeacherClassroomData(String startDate, String endDate) {
        TeacherClassroomData classroomData =new TeacherClassroomData();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Specification<ClassroomExercises> specification = new Specification<ClassroomExercises>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate condition = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition = criteriaBuilder.between(root.<Date>get("publishTime"),startDate1,endDate1);
                        list.add(condition);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<ClassroomExercises> exercisesList =classroomExercisesRepository.findAll(specification);

        classroomData.setClassesNum(exercisesList.size());
        classroomData.setClassroomExercisesNum(exercisesList.size());
        Specification<ClassroomExercisesStudentRecord> specification1 = new Specification<ClassroomExercisesStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate condition = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
                        list.add(condition);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<ClassroomExercisesStudentRecord> exercisesStudentRecordList=classroomExercisesStudentRecordRepository.findAll(specification1);
        int purePenPlowNum = 0;
        for(ClassroomExercisesStudentRecord record:exercisesStudentRecordList){
            if(record.getStudentWriteDataList()!=null&&record.getStudentWriteDataList().size()>0){
                purePenPlowNum++;
            }
        }
        classroomData.setPurePenPlowNum(purePenPlowNum);
        classroomData.setClassroomInteractionNum(exercisesStudentRecordList.size());
        Map<String,Integer> dayClassroomUseNumMap = new HashMap<>();
        for(ClassroomExercises exercises:exercisesList){
            String day = sdf.format(exercises.getPublishTime());
            if(dayClassroomUseNumMap.containsKey(day)){
                dayClassroomUseNumMap.put(day,dayClassroomUseNumMap.get(day)+1);
            }else {
                dayClassroomUseNumMap.put(day,1);
            }
        }
        classroomData.setDayClassroomUseNumMap(dayClassroomUseNumMap);
        return classroomData;
    }

    @Override
    public Page<ClassroomExercises> classInteractList(Integer pageNum, Integer pageSize, Long classId, String homeworkName, String startDate, String endDate, Integer exercisesType) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Specification<ClassroomExercises> specification = new Specification<ClassroomExercises>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(classId!=null){
                        Predicate con = criteriaBuilder.like(root.get("classIds").as(String.class),"%"+classId+"%");
                        list.add(con);
                    }
                    if(StringUtils.isNotEmpty(homeworkName)){
                        Predicate con1 = criteriaBuilder.like(root.get("homeworkName").as(String.class),"%"+homeworkName+"%");
                        list.add(con1);
                    }
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        Predicate condition = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
                        list.add(condition);
                    }
                    if(exercisesType!=null){
                        Predicate con2 = criteriaBuilder.equal(root.get("exercisesType"),exercisesType);
                        list.add(con2);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Page<ClassroomExercises> page=  classroomExercisesRepository.findAll(specification,pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        for(ClassroomExercises item:exercisesList){
            List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.selectQuestionList(item.getId());
            item.setQuestionList(questionList);
        }
        return page;
    }


}
