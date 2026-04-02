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
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import com.jlm.homework.util.DateUtil;

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
        ClassroomExercises finalClassroomExercises = classroomExercises;
        questionList.stream().forEach(question->{
            question.setClassIds(finalClassroomExercises.getClassIds());
            question.setClassNames(finalClassroomExercises.getClassNames());
        });
        if(classroomExercises.getSchoolId()==null){
            classroomExercises.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        classroomExercises.setExercisesType(1);//随堂检测
        classroomExercises.setCreateTime(new Date());
        classroomExercises.setUseStatus(0);
        if("math".equals(classroomExercises.getSubject())){
            classroomExercises.setSubject("数学");
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
        classroomExercises.setDeleteFlag(0);
        Page<ClassroomExercises> page= classroomExercisesRepository.findAll(Example.of(classroomExercises),pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        
        // 批量查询问题列表
        if (!exercisesList.isEmpty()) {
            List<Long> exercisesIds = exercisesList.stream().map(ClassroomExercises::getId).collect(Collectors.toList());
            List<ClassroomExercisesQuestion> allQuestions = classroomExercisesQuestionService.selectQuestionListByExercisesIds(exercisesIds);
            
            // 按课堂练习 ID 分组
            Map<Long, List<ClassroomExercisesQuestion>> questionMap = new HashMap<>();
            for (ClassroomExercisesQuestion question : allQuestions) {
                questionMap.computeIfAbsent(question.getClassroomExercisesId(), k -> new ArrayList<>()).add(question);
            }
            
            // 设置问题列表到对应的课堂练习对象
            for (ClassroomExercises item : exercisesList) {
                item.setQuestionList(questionMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }

        return page;
    }

    @Override
    public void deleteById(Long id) {
        ClassroomExercises classroomExercises=classroomExercisesRepository.findById(id).orElse(null);
        if(classroomExercises!=null){
            classroomExercises.setDeleteFlag(1);
            classroomExercisesRepository.save(classroomExercises);
        }
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
        Map<String,String> questionIdMap = new HashMap<>();
        for(ClassroomExercisesQuestion question:questionList){
            if(StringUtils.isNotEmpty(question.getQuestionType())) {
                if (exerciseTypeMap.containsKey(question.getQuestionType())) {
                    exerciseTypeMap.put(question.getQuestionType(), exerciseTypeMap.get(question.getQuestionType()) + 1);
                } else {
                    exerciseTypeMap.put(question.getQuestionType(), 1);
                }
                if (questionIdMap.containsKey(question.getQuestionType())) {
                    questionIdMap.put(question.getQuestionType(), questionIdMap.get(question.getQuestionType()) + "," + question.getId());
                } else {
                    questionIdMap.put(question.getQuestionType(), "" + question.getId());
                }
                if(question.getCreateTime()!=null) {
                    String day = DateUtil.formatDate(question.getCreateTime());
                    String key = question.getQuestionType() + ":" + day;
                    if (dayExerciseTypeMap.containsKey(key)) {
                        dayExerciseTypeMap.put(key, dayExerciseTypeMap.get(key) + 1);
                    } else {
                        dayExerciseTypeMap.put(key, 1);
                    }
                }
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
                    if(studentAnswer.getRightFlag()!=null) {
                        if (1 == studentAnswer.getRightFlag()) {
                            rightNum++;
                        } else if (0 == studentAnswer.getRightFlag()) {
                            wrongNum++;
                        }
                    }
                }
                exerciseTypeErrorRate.setRightNum(rightNum);
                Double errorRate = BigDecimal.valueOf(wrongNum).divide(BigDecimal.valueOf(studentAnswerList.size()), 4, BigDecimal.ROUND_HALF_UP)
                        .doubleValue();
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
        Integer useStatus = 0;
        if(classroomExercisesId==0&&schoolId==null){//老师黑板现场出题
            schoolId = userService.getCurrentSchoolIdSafely();

        }else if(classroomExercisesId!=0){
            classroomExercises=classroomExercisesRepository.findById(classroomExercisesId).get();
            if(classroomExercises.getUseStatus()!=null) {
                useStatus = classroomExercises.getUseStatus();
            }
            classroomExercises.setUseStatus(1);
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
        boolean saveflag = true;
        String className = null;
        if(classroomExercisesId==null||classroomExercisesId == 0||(1==exercisesType&&useStatus!=null&&1==useStatus)){
            CurrentUserInfo userInfo=userService.getCurrentUserInfo();
            String teacherName ="";
            if(userInfo!=null){
                teacherName = userService.getCurrentUserInfo().getTeacherName();
            }


            classroomExercises.setClassIds(Arrays.asList(classId));
            classroomExercises.setExercisesType(exercisesType);
            classroomExercises.setSchoolId(schoolId);
            classroomExercises.setDeleteFlag(0);
            List<ClassroomExercisesQuestion> exercisesQuestionList= null;
            if(2==exercisesType){
                classroomExercises.setHomeworkName(teacherName+DateUtil.formatDateTime(new Date())+"课堂互动");
            }else if(3==exercisesType){
                classroomExercises.setHomeworkName(teacherName+DateUtil.formatDateTime(new Date())+"纸笔直播");
            }else if(1==exercisesType&&1==useStatus&&classroomExercisesId!=null&&classroomExercisesId!=0){
                saveflag = false;
                exercisesQuestionList=classroomExercisesQuestionService.selectQuestionList(classroomExercisesId);
                ClassroomExercises exercises = new ClassroomExercises();
                exercises.setParentId(classroomExercisesId);
                exercises.setId(null);
                ClassroomExercises countSaerch = new ClassroomExercises();
                countSaerch.setParentId(classroomExercisesId);
                long count=classroomExercisesRepository.count(Example.of(countSaerch));
                exercises.setHomeworkName(classroomExercises.getHomeworkName()+"-复测"+(count+1));
                exercises.setExercisesType(exercises.getExercisesType());
                exercises.setGradeId(classroomExercises.getGradeId());
                exercises.setGradeName(classroomExercises.getGradeName());
                exercises.setClassIds(classroomExercises.getClassIds());
                exercises.setClassNames(classroomExercises.getClassNames());
                exercises.setSchoolId(classroomExercises.getSchoolId());
                exercises.setDeleteFlag(0);
                exercises.setScheduledReleaseFlag(0);
                exercises.setSubject(classroomExercises.getSubject());
                exercises.setPublishTime(new Date());
                exercises.setCreateTime(new Date());
                exercises.setTestSource(classroomExercises.getTestSource());
                exercises.setPublishStatus(classroomExercises.getPublishStatus());
                exercises.setUseStatus(1);
                exercises.setExercisesType(1);
                exercises=classroomExercisesRepository.save(exercises);
                //classroomExercises.setId(exercises.getId());
                classroomExercisesId = exercises.getId();
                if(exercisesQuestionList!=null&&exercisesQuestionList.size()>0){
                    for(ClassroomExercisesQuestion question:exercisesQuestionList){
                        ClassroomExercisesQuestion cpQuestion = new  ClassroomExercisesQuestion();
                        cpQuestion.setId(null);
                        cpQuestion.setClassroomExercisesId(classroomExercisesId);
                        cpQuestion.setCreateTime(new Date());
                        cpQuestion.setOptions(question.getOptions());
                        cpQuestion.setParse(question.getParse());
                        cpQuestion.setAnswer(question.getAnswer());
                        cpQuestion.setQuestionType(question.getQuestionType());
                        cpQuestion.setDifficulty(question.getDifficulty());
                        cpQuestion.setClassIds(question.getClassIds());
                        cpQuestion.setClassNames(question.getClassNames());
                        cpQuestion.setKnowledgePoint(question.getKnowledgePoint());
                        cpQuestion.setQuestionBankId(question.getQuestionBankId());
                        cpQuestion.setSubject(question.getSubject());
                        cpQuestion.setQuestionContent(question.getQuestionContent());
                        cpQuestion.setTitleNumber(question.getTitleNumber());
                        classroomExercisesQuestionService.saveQuestion(cpQuestion);
                    }
                }
            }
            classroomExercises.setUseStatus(1);
            if(saveflag) {
                classroomExercises.setCreateTime(new Date());
                classroomExercises = classroomExercisesRepository.save(classroomExercises);
                classroomExercisesId = classroomExercises.getId();
            }



        }else{
            ClassroomExercisesStudentRecord studentRecord=new ClassroomExercisesStudentRecord();
            studentRecord.setClassId(classId);
            studentRecord.setClassroomExercisesId(classroomExercisesId);
            List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(studentRecord));
            if(recordList!=null&&recordList.size()>0){
                classroomExercises.setUseStatus(1);
                classroomExercisesRepository.save(classroomExercises);
                return classroomExercisesId;
            }
        }
        if(studentList!=null&&studentList.size()>0){
            Student student = studentList.get(0);
            classroomExercises.setUseStatus(1);
            if(classroomExercises.getClassNames()==null||classroomExercises.getClassNames().size()==0){
                classroomExercises.setClassNames(Arrays.asList(student.getClassesName()));
                classroomExercises.setGradeId(student.getGradeId());
                classroomExercises.setGradeName(student.getGradeName());
                classroomExercises.setUseStatus(1);

            }
            classroomExercisesRepository.save(classroomExercises);
        }

        // 批量保存学生记录
        List<ClassroomExercisesStudentRecord> recordList = new ArrayList<>();
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
            recordList.add(record);
        }
        if (!recordList.isEmpty()) {
            classroomExercisesStudentRecordRepository.saveAll(recordList);
        }
        return classroomExercisesId;
    }

    /**
     * 构建课堂练习查询的 Specification
     */
    private Specification<ClassroomExercises> buildClassroomExercisesSpecification(Long classId, String homeworkName, String startDate, String endDate, Integer exercisesType, Integer useStatus) {
        return new Specification<ClassroomExercises>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    // 基础条件：未删除
                    Predicate condDel = criteriaBuilder.equal(root.get("deleteFlag"), 0);
                    list.add(condDel);
                    
                    // 学校 ID
                    Predicate condSchool = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                    list.add(condSchool);
                    
                    // 班级 ID
                    if (classId != null) {
                        Predicate condClass = criteriaBuilder.like(root.get("classIds").as(String.class), "%" + classId + "%");
                        list.add(condClass);
                    }
                    
                    // 作业名称
                    if (StringUtils.isNotEmpty(homeworkName)) {
                        Predicate condHomeworkName = criteriaBuilder.like(root.get("homeworkName").as(String.class), "%" + homeworkName + "%");
                        list.add(condHomeworkName);
                    }
                    
                    // 时间范围
                    if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
                        Date startDate1 = DateUtil.parseDateTime(startDate);
                        Date endDate1 = DateUtil.parseDateTime(endDate);
                        Predicate condDate = criteriaBuilder.between(root.<Date>get("createTime"), startDate1, endDate1);
                        list.add(condDate);
                    }
                    
                    // 练习类型
                    if (exercisesType != null) {
                        Predicate condType = criteriaBuilder.equal(root.get("exercisesType"), exercisesType);
                        list.add(condType);
                    }
                    
                    // 使用状态
                    if (useStatus != null) {
                        Predicate condUseStatus = criteriaBuilder.equal(root.get("useStatus"), useStatus);
                        list.add(condUseStatus);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("构建课堂练习查询条件失败: " + e.getMessage(), e);
                }
                Predicate[] p = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
    }

    @Override
    public TeacherClassroomData getTeacherClassroomData(String startDate, String endDate) {
        TeacherClassroomData classroomData =new TeacherClassroomData();
        Specification<ClassroomExercises> specification = new Specification<ClassroomExercises>() {
            @Override
            public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate condition = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = DateUtil.parseDate(startDate);
                        Date endDate1 = DateUtil.parseDate(endDate);
                        condition = criteriaBuilder.between(root.<Date>get("publishTime"),startDate1,endDate1);
                        list.add(condition);
                    }
                    Predicate condition1 = criteriaBuilder.equal(root.get("useStatus"),1);
                    list.add(condition1);
                } catch (Exception e) {
                    throw new RuntimeException("构建课堂练习统计查询条件失败: " + e.getMessage(), e);
                }
                Predicate condDel = criteriaBuilder.equal(root.get("deleteFlag"),0);
                list.add(condDel);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<ClassroomExercises> exercisesList =classroomExercisesRepository.findAll(specification);

        classroomData.setClassesNum(exercisesList.size());
        classroomData.setClassroomExercisesNum(exercisesList.size());
        int purePenPlowNum = 0;
        int classroomExercisesNum = 0;
        int classroomInteractionNum =0;
        for(ClassroomExercises exercises :exercisesList){
            if(exercises.getExercisesType()!=null) {
                if (1 == exercises.getExercisesType()) {
                    classroomExercisesNum++;
                } else if (2 == exercises.getExercisesType()) {
                    classroomInteractionNum++;
                } else if (3 == exercises.getExercisesType()) {
                    purePenPlowNum++;
                }
            }
        }
        classroomData.setClassroomExercisesNum(classroomExercisesNum);
        classroomData.setPurePenPlowNum(purePenPlowNum);
        classroomData.setClassroomInteractionNum(classroomInteractionNum);
        Map<String,Integer> dayClassroomUseNumMap = new HashMap<>();
        for(ClassroomExercises exercises:exercisesList){
            if(exercises.getPublishTime()!=null) {
                String day = DateUtil.formatDate(exercises.getPublishTime());
                if (dayClassroomUseNumMap.containsKey(day)) {
                    dayClassroomUseNumMap.put(day, dayClassroomUseNumMap.get(day) + 1);
                } else {
                    dayClassroomUseNumMap.put(day, 1);
                }
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
        
        // 使用公共方法构建 Specification
        Specification<ClassroomExercises> specification = buildClassroomExercisesSpecification(classId, homeworkName, startDate, endDate, exercisesType, 1);
        
        Page<ClassroomExercises> page=  classroomExercisesRepository.findAll(specification,pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        
        // 批量查询问题列表
        if (!exercisesList.isEmpty()) {
            List<Long> exercisesIds = exercisesList.stream().map(ClassroomExercises::getId).collect(Collectors.toList());
            List<ClassroomExercisesQuestion> allQuestions = classroomExercisesQuestionService.selectQuestionListByExercisesIds(exercisesIds);
            
            // 按课堂练习 ID 分组
            Map<Long, List<ClassroomExercisesQuestion>> questionMap = new HashMap<>();
            for (ClassroomExercisesQuestion question : allQuestions) {
                questionMap.computeIfAbsent(question.getClassroomExercisesId(), k -> new ArrayList<>()).add(question);
            }
            
            // 设置问题列表到对应的课堂练习对象
            for (ClassroomExercises item : exercisesList) {
                item.setQuestionList(questionMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }
        return page;
    }

    @Override
    public Page<ClassroomExercises> selectPurchaseList(Integer pageNum, Integer pageSize, Long classId, String homeworkName, String startTime, String endTime, Integer exercisesType) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 100 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        
        // 使用公共方法构建 Specification
        Specification<ClassroomExercises> specification = buildClassroomExercisesSpecification(classId, homeworkName, startTime, endTime, exercisesType, null);
        
        Page<ClassroomExercises> page=  classroomExercisesRepository.findAll(specification,pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        
        // 批量查询问题列表
        if (!exercisesList.isEmpty()) {
            List<Long> exercisesIds = exercisesList.stream().map(ClassroomExercises::getId).collect(Collectors.toList());
            List<ClassroomExercisesQuestion> allQuestions = classroomExercisesQuestionService.selectQuestionListByExercisesIds(exercisesIds);
            
            // 按课堂练习 ID 分组
            Map<Long, List<ClassroomExercisesQuestion>> questionMap = new HashMap<>();
            for (ClassroomExercisesQuestion question : allQuestions) {
                questionMap.computeIfAbsent(question.getClassroomExercisesId(), k -> new ArrayList<>()).add(question);
            }
            
            // 设置问题列表到对应的课堂练习对象
            for (ClassroomExercises item : exercisesList) {
                item.setQuestionList(questionMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        }
        return page;
    }


}
