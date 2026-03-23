package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.SchoolFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.repository.StudentsHomeworkStatisticsRepository;
import com.jlm.homework.service.ExerciseBookServer;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.IStudentsHomeworkStatisticsService;
import jakarta.annotation.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.jlm.homework.util.DateUtil;
import java.util.*;

@Service
public class StudentsHomeworkStatisticsServiceImpl implements IStudentsHomeworkStatisticsService {
    @Resource
    private StudentsHomeworkStatisticsRepository studentsHomeworkStatisticsRepository;
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Autowired
    private SchoolFeignClient schoolFeignClient;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
    @Override
    public StudentsHomeworkStatistics getStudentsHomeworkStatistics(Long homeworkPublishId,Long classId) {
        StudentsHomeworkStatistics studentsHomeworkStatistics = new StudentsHomeworkStatistics();
        studentsHomeworkStatistics.setHomeworkPublishId(homeworkPublishId);
        studentsHomeworkStatistics.setClassId(classId);
        Example<StudentsHomeworkStatistics> example=Example.of(studentsHomeworkStatistics);
        Optional<StudentsHomeworkStatistics> optional= studentsHomeworkStatisticsRepository.findOne(example);
        if(optional.isEmpty()){
            return studentsHomeworkStatistics;
        }
        return optional.get();
    }

    @Override
    public Long insertStudentHomeworkStatistics(StudentsHomeworkStatistics studentsHomeworkStatistics) {
        studentsHomeworkStatistics=studentsHomeworkStatisticsRepository.save(studentsHomeworkStatistics);
        return studentsHomeworkStatistics.getId();
    }

    public void addStudentHomeworkStatistics(Long homeworkPublishId,Long classId) {
        StudentsHomeworkNew search=new StudentsHomeworkNew();
        search.setHomeworkPublishId(homeworkPublishId);
        search.setClassesId(classId);
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(Example.of(search));
        StudentsHomeworkStatistics  studentsHomeworkStatistics = new StudentsHomeworkStatistics();
        studentsHomeworkStatistics.setHomeworkPublishId(homeworkPublishId);
        studentsHomeworkStatistics.setClassId(classId);
        studentsHomeworkStatistics.setStudentsSum(studentsHomeworkList.size());
        Integer submitStudentNum=0;

        Integer unsubmitStudentNum=0;
        Double fastestDuration=0.0;
        Double slowestDuration=0.0;
        Double totalDuration=0.0;
        Double totalAccuracy=0.0;
        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            if(1==studentsHomework.getSubmitStatus()){
                submitStudentNum++;
            }else{
                unsubmitStudentNum++;
            }

            if(studentsHomework.getStartTime()!=null&&studentsHomework.getSubmitTime()!=null ){
                double duration = 0.0;
                duration =  (studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime())/1000/60;
                totalDuration  += duration;
                if(duration!=0.0&&duration<fastestDuration){
                    fastestDuration = duration;
                }
                if(duration!=0.0&&duration>slowestDuration){
                    slowestDuration = duration;
                }
            }
            if(studentsHomework.getAccuracy()!=null){
                totalAccuracy +=studentsHomework.getAccuracy();
            }
        }
        studentsHomeworkStatistics.setSubmitStudentNum(submitStudentNum);
        studentsHomeworkStatistics.setUnsubmitStudentNum(unsubmitStudentNum);
        Double submitRate = 0.0d;
        if(studentsHomeworkList.size()>0){
            submitRate = BigDecimal.valueOf(submitStudentNum).divide(BigDecimal.valueOf(studentsHomeworkList.size()),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        Double unsubmitRate = 0.0d;
        if(studentsHomeworkList.size()>0){
            unsubmitRate = BigDecimal.valueOf(unsubmitStudentNum).divide(BigDecimal.valueOf(studentsHomeworkList.size()),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        studentsHomeworkStatistics.setSubmitRate(submitRate);
        studentsHomeworkStatistics.setUnsubmitRate(unsubmitRate);
        studentsHomeworkStatistics.setFastestDuration(fastestDuration);
        studentsHomeworkStatistics.setSlowestDuration(slowestDuration);
        if(submitStudentNum!=null&&submitStudentNum!=0){
            studentsHomeworkStatistics.setAverageDuration(totalDuration/submitStudentNum);
        }
        if(studentsHomeworkList.size()!=0) {
            studentsHomeworkStatistics.setAverageAccuracy(totalAccuracy / studentsHomeworkList.size());
            studentsHomeworkStatistics.setAverageCorrectness(totalAccuracy / studentsHomeworkList.size());
        }
        studentsHomeworkStatistics.setCompareLast(0.0);
        studentsHomeworkStatisticsRepository.save(studentsHomeworkStatistics);
    }

    @Override
    public SchoolHomeworkData getSchoolHomeworkData(Long schoolId) {
        SchoolHomeworkData schoolHomeworkData= new SchoolHomeworkData();
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list =  new ArrayList<>();

                if(schoolId!=null){
                    Predicate condition1 = criteriaBuilder.equal(root.get("schoolId"), schoolId);
                    list.add(condition1);
                }
                Predicate condition = criteriaBuilder.isNotNull(root.get("submitStatus"));
                list.add(condition);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=studentsHomeworkNewService.findAllSimpleDTOBySpecification(specification);
        if(studentsHomeworkList==null||studentsHomeworkList.isEmpty()){
            return schoolHomeworkData;
        }

        Integer totleNum=0;
        Integer submittedNum=0;
        Integer unsubmittedNum=0;
        Map<String,Integer> gradeSubmitMap=new HashMap<>();
        Map<String,Double> gradeRightRate=new HashMap<>();
        Map<String,Integer> gradeUnSubmitMap=new HashMap<>();
        Map<String,Integer> gradeTotalMap=new HashMap<>();
        Map<Integer,Integer> homeworkNumMap = new HashMap<>();
        Map<Integer,Integer> auditNumMap = new HashMap<>();
        String today = DateUtil.formatDate(new Date());
        Map<Long,String> classMap = new HashMap<>();
        Map<String,Integer> classTotalMap = new HashMap<>();
        Map<String,Integer> classSubmitMap = new HashMap<>();
        List<Long> publishHomeworkIdList=new ArrayList<>();

        for(StudentsHomeworkSimpleDTO studentsHomework:studentsHomeworkList){
            totleNum++;
            gradeTotalMap.merge(studentsHomework.getGrade(), 1, Integer::sum);
            if(studentsHomework.getSubmitStatus()!=null&&1==studentsHomework.getSubmitStatus()){
                submittedNum++;
                gradeSubmitMap.merge(studentsHomework.getGrade(), 1, Integer::sum);
            }else{
                unsubmittedNum++;
                gradeUnSubmitMap.merge(studentsHomework.getGrade(), 1, Integer::sum);
            }
            if(studentsHomework.getSubmitTime()!=null){
                Integer m;
                if(studentsHomework.getStartTime()!=null) {
                    m = Math.toIntExact((studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime()) / 1000 / 60);
                }else{
                    m = Math.toIntExact((studentsHomework.getSubmitTime().getTime() - studentsHomework.getCreateTime().getTime()) / 1000 / 60);
                }
                if(m<=10){
                    homeworkNumMap.merge(10, 1, Integer::sum);
                }else if(m<=30){
                    homeworkNumMap.merge(30, 1, Integer::sum);
                }else if(m<=60){
                    homeworkNumMap.merge(60, 1, Integer::sum);
                }else if(m<=90){
                    homeworkNumMap.merge(90, 1, Integer::sum);
                }else if(m<=120){
                    homeworkNumMap.merge(120, 1, Integer::sum);
                }
            }
            if(studentsHomework.getSubmitTime()!=null&&studentsHomework.getAuditTime()!=null){
                Integer m = Math.toIntExact((studentsHomework.getAuditTime().getTime() - studentsHomework.getSubmitTime().getTime()) / 1000 / 60);
                if(m<=10){
                    auditNumMap.merge(10, 1, Integer::sum);
                }else if(m<=30){
                    auditNumMap.merge(30, 1, Integer::sum);
                }else if(m<=60){
                    auditNumMap.merge(60, 1, Integer::sum);
                }else if(m<=90){
                    auditNumMap.merge(90, 1, Integer::sum);
                }else if(m<=120){
                    auditNumMap.merge(120, 1, Integer::sum);
                }



            }
            //今日
            String createDate = DateUtil.formatDate(studentsHomework.getCreateTime());
            String className = studentsHomework.getClassesName();
            if(!classMap.containsKey(studentsHomework.getClassesId())){
                classMap.put(studentsHomework.getClassesId(),className);
            }
            if(today.equals(createDate)){
                classTotalMap.merge(className, 1, Integer::sum);
            }
            if(studentsHomework.getSubmitTime()!=null){
                String day = DateUtil.formatDate(studentsHomework.getSubmitTime());
                if(day.equals(today)){
                    classSubmitMap.merge(className, 1, Integer::sum);
                }
            }
            if(studentsHomework.getAuditTime()!=null){
                String auditDate = DateUtil.formatDate(studentsHomework.getAuditTime());
                if(auditDate.equals(today)&&!publishHomeworkIdList.contains(studentsHomework.getHomeworkPublishId())){
                    publishHomeworkIdList.add(studentsHomework.getHomeworkPublishId());
                }
            }

        }
        //作业提交
        Map<String,Double> homeworkSubmitSituation = new HashMap<>();
        Double totalSubmitRate = BigDecimal.valueOf(submittedNum).divide(BigDecimal.valueOf(totleNum),4,BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        homeworkSubmitSituation.put("已提交",totalSubmitRate);
        Double totalUnsubmitRate = BigDecimal.valueOf(unsubmittedNum).divide(BigDecimal.valueOf(totleNum),4,BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        homeworkSubmitSituation.put("未提交",totalUnsubmitRate);
        schoolHomeworkData.setHomeworkSubmitSituation(homeworkSubmitSituation);
        List<GradeHomeworkSubmit> gradeSubmitSituation = new ArrayList<>();
        Map<String, Object> totalRightRateMap = studentsHomeworkStatisticsRepository.getRightRatetotal(schoolId);
        HomeworkRightRate totalRightRate = new HomeworkRightRate();
        if(totalRightRateMap!=null){
            // 安全地将Number转换为Integer
            if(totalRightRateMap.containsKey("totalNum")&&totalRightRateMap.get("totalNum")!=null){
                totalRightRate.setTotalNum(((Number) totalRightRateMap.get("totalNum")).intValue());
            }else{
                totalRightRate.setTotalNum(0);
            }
            if(totalRightRateMap.containsKey("rightNum")&&totalRightRateMap.get("rightNum")!=null){
                totalRightRate.setRightNum(((Number) totalRightRateMap.get("rightNum")).intValue());
            }else{
                totalRightRate.setRightNum(0);
            }
            if(totalRightRateMap.containsKey("errorNum")&&totalRightRateMap.get("errorNum")!=null){
                totalRightRate.setErrorNum(((Number) totalRightRateMap.get("errorNum")).intValue());
            }else{
                totalRightRate.setErrorNum(0);
            }
            Double rightRate = 0.0;
            if(totalRightRate.getTotalNum()!=null&&totalRightRate.getRightNum()!=null
                    &&totalRightRate.getTotalNum()!=0){
                rightRate =  BigDecimal.valueOf(totalRightRate.getRightNum()).divide(BigDecimal.valueOf(totalRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                totalRightRate.setRightRate(rightRate);
            }
            Double erroRate =0.0;
            if(totalRightRate.getTotalNum()!=null&&totalRightRate.getErrorNum()!=null
                    &&totalRightRate.getTotalNum()!=0){
                erroRate =  BigDecimal.valueOf(totalRightRate.getErrorNum()).divide(BigDecimal.valueOf(totalRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                totalRightRate.setErrorRate(erroRate);
            }
            Double noAnswerRate = 100.0 - rightRate -erroRate;
            totalRightRate.setNoAnswerRate(noAnswerRate);
        }
        schoolHomeworkData.setTotalRightRate(totalRightRate);
        List<Map<String, Object>> gradesRightRateMapList = studentsHomeworkStatisticsRepository.getRightRate(schoolId);
        List<HomeworkRightRate> gradesRightRateList = new ArrayList<>();
        if(gradesRightRateMapList!=null&&gradesRightRateMapList.size()>0){
            for(Map<String, Object> rightRateMap:gradesRightRateMapList){
                HomeworkRightRate homeworkRightRate = new HomeworkRightRate();
                Long classId =(Long) rightRateMap.get("classId");
                homeworkRightRate.setClassId(classId);
                if(classMap.containsKey(classId)) {
                    homeworkRightRate.setClassName(classMap.get(classId));
                }
                homeworkRightRate.setGrade((String) rightRateMap.get("grade"));
                // 安全地将Number转换为Integer
                if(rightRateMap.containsKey("totalNum")&&rightRateMap.get("totalNum")!=null){
                    homeworkRightRate.setTotalNum(((Number) rightRateMap.get("totalNum")).intValue());
                }else{
                    homeworkRightRate.setTotalNum(0);
                }
                if(rightRateMap.containsKey("rightNum")&&rightRateMap.get("rightNum")!=null){
                    homeworkRightRate.setRightNum(((Number) rightRateMap.get("rightNum")).intValue());
                }else{
                    homeworkRightRate.setRightNum(0);
                }
                if(rightRateMap.containsKey("errorNum")&&rightRateMap.get("errorNum")!=null){
                    homeworkRightRate.setErrorNum(((Number) rightRateMap.get("errorNum")).intValue());
                }else{
                    homeworkRightRate.setErrorNum(0);
                }
                Double rightRate = 0.0;
                if(homeworkRightRate.getTotalNum()!=null&&homeworkRightRate.getRightNum()!=null
                        &&homeworkRightRate.getTotalNum()!=0){
                    rightRate =  BigDecimal.valueOf(homeworkRightRate.getRightNum()).divide(BigDecimal.valueOf(homeworkRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    homeworkRightRate.setRightRate(rightRate);
                }
                Double erroRate = 0.0;
                if(homeworkRightRate.getTotalNum()!=null&&homeworkRightRate.getErrorNum()!=null
                        &&homeworkRightRate.getTotalNum()!=0){
                    erroRate =  BigDecimal.valueOf(homeworkRightRate.getErrorNum()).divide(BigDecimal.valueOf(homeworkRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    homeworkRightRate.setErrorRate(erroRate);
                }
                Double noAnswerRate = 100.0 - rightRate -erroRate;
                totalRightRate.setNoAnswerRate(noAnswerRate);
                gradesRightRateList.add(homeworkRightRate);
            }
        }
        schoolHomeworkData.setGradesRightRateList(gradesRightRateList);
        for(String grade:gradeTotalMap.keySet()) {
            HomeworkRightRate homeworkRightRate = new HomeworkRightRate();
            GradeHomeworkSubmit gradeHomeworkSubmit = new GradeHomeworkSubmit();
            gradeHomeworkSubmit.setGrade(grade);
            homeworkRightRate.setClassName(grade);
            Double gradeSubmitRate = 0.0d;
            if (gradeTotalMap != null && gradeTotalMap.get(grade) != null && gradeTotalMap.get(grade) != 0
                    &&gradeSubmitMap !=null &&gradeSubmitMap.containsKey(grade) &&gradeSubmitMap.get(grade) != null){
                gradeSubmitRate = BigDecimal.valueOf(gradeSubmitMap.get(grade)).divide(BigDecimal.valueOf(gradeTotalMap.get(grade)), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            gradeHomeworkSubmit.setSubmitRate(gradeSubmitRate);
            Double gradeUnsubmitRate = 0.0d;
            if (gradeTotalMap != null && gradeTotalMap.containsKey(grade) && gradeTotalMap.get(grade) != 0
                    && gradeUnSubmitMap!=null &&gradeUnSubmitMap.containsKey(grade) && gradeUnSubmitMap.get(grade) != null) {
                gradeUnsubmitRate = BigDecimal.valueOf(gradeUnSubmitMap.get(grade)).divide(BigDecimal.valueOf(gradeTotalMap.get(grade)), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            gradeHomeworkSubmit.setUnSubmitRate(gradeUnsubmitRate);
            gradeSubmitSituation.add(gradeHomeworkSubmit);
        }
        schoolHomeworkData.setGradeSubmitSituation(gradeSubmitSituation);
        schoolHomeworkData.setHomeworkNumMap(homeworkNumMap);
        schoolHomeworkData.setTeacherAuditMap(auditNumMap);
        //今日提交
        List<TodayHomeworkSubmit>  todayHomeworkSubmit = new ArrayList<>();
        for(String className : classTotalMap.keySet()){
            TodayHomeworkSubmit homeworkSubmit = new TodayHomeworkSubmit();
            homeworkSubmit.setClassName(className);
            homeworkSubmit.setStudentNum(classTotalMap.get(className));
            if(classSubmitMap!=null&&classSubmitMap.containsKey(className)) {
                homeworkSubmit.setSubmitNum(classSubmitMap.get(className));
            }else{
                homeworkSubmit.setSubmitNum(0);
            }
            Double submitRate = 0.0d;
            if(classSubmitMap!=null&&classSubmitMap.containsKey(className)&&classSubmitMap.get(className) != null
                    &&classTotalMap.get(className)!=0){
                submitRate = BigDecimal.valueOf(classSubmitMap.get(className))
                        .divide(BigDecimal.valueOf(classTotalMap.get(className)),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            homeworkSubmit.setSubmitRate(submitRate);
            todayHomeworkSubmit.add(homeworkSubmit);
        }
        schoolHomeworkData.setTodayHomeworkSubmit(todayHomeworkSubmit);

        //今日审批
        List<HomeworkPublish> publishList=new ArrayList<>();
        if(!publishHomeworkIdList.isEmpty()){
            publishList = homeworkPublishRepository.findAllById(publishHomeworkIdList);
        }
        schoolHomeworkData.setTodayHomeworkAuditList(publishList);
        ExerciseBookRequest exerciseBookRequest = new ExerciseBookRequest();
        exerciseBookRequest.setSchoolId(schoolId);
        Page<ExerciseBookEntity> page=exerciseBookServer.searchExerciseBooks(exerciseBookRequest);
        //练习册使用情况
        ExerciseBookUseDate exerciseBookUse = new ExerciseBookUseDate();
        exerciseBookUse.setExerciseBookNum(page.getNumberOfElements());
        HomeworkPublish schoolHomeworkPublish = new HomeworkPublish();
        schoolHomeworkPublish.setSchoolId(schoolId);
        List<HomeworkPublish> publishList1=homeworkPublishRepository.findAll(Example.of(schoolHomeworkPublish));
        Map<String,Integer> map = new HashMap<>();
        for(HomeworkPublish homeworkPublish1:publishList1){
            if(StringUtils.isNotEmpty(homeworkPublish1.getExerciseBookName())) {
                map.merge(homeworkPublish1.getExerciseBookName(), 1, Integer::sum);
            }
        }
        exerciseBookUse.setExerciseBookUsedum(map.keySet().size());
        String maxUsedBookName="";
        String minUsedBookName="";
        int maxUsedBookNum=0;
        int minUsedBookNum=0;
        for(String key:map.keySet()){
            if(map.get(key)>=maxUsedBookNum){
                maxUsedBookName=key;
            }
            if(map.get(key)<=minUsedBookNum){
                minUsedBookName = key;
            }
        }
        exerciseBookUse.setMaxUsedBookName(maxUsedBookName);
        exerciseBookUse.setMinUsedBookName(minUsedBookName);
        schoolHomeworkData.setExerciseBookUse(exerciseBookUse);
        return schoolHomeworkData;
    }

    @Override
    public EducHomeworkData getEducHomeworkData(Long educOrgId,Long schoolId,String schoolType ) {
        if(educOrgId==null&&schoolId!=null){
            ResultDto<SysSchool> resultDto= schoolFeignClient.getInfo(schoolId);
            if(resultDto!=null&&resultDto.getData()!=null){
                educOrgId = resultDto.getData().getEducOrgId();
            }
        }
        EducHomeworkData educHomeworkData=new  EducHomeworkData();
        List<SysSchool> schoolList=schoolFeignClient.getInfoByEducOrg(educOrgId,schoolType);
        List<Long> schoolIdList =schoolList.stream().map(SysSchool::getSchoolId).toList();
        educHomeworkData.setSchoolNum(schoolList.size());
        Integer studentNum=0;
        List<SchoolHomeworkNum> schoolHomeworkNumList = new ArrayList<>();
        for(SysSchool school:schoolList){
            SchoolHomeworkNum  schoolHomeworkNum=new SchoolHomeworkNum();
            schoolHomeworkNum.setSchoolId(school.getSchoolId());
            schoolHomeworkNum.setSchoolName(school.getSchoolName());
            schoolHomeworkNum.setSchoolAdress(school.getAddress());
            Result<Student> result = studentFeignClient.getStudentList(1,100,school.getSchoolId(),null,null,"0");
            if(result!=null&result.getRows()!=null){
                studentNum += result.getRows().size();
                schoolHomeworkNum.setStudentNum(result.getRows().size());
            }
            schoolHomeworkNumList.add(schoolHomeworkNum);
        }
        Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(!schoolIdList.isEmpty()){
                    CriteriaBuilder.In<Object> in = criteriaBuilder.in(root.get("schoolId"));
                    for(Long schoolId:schoolIdList){
                        in.value(schoolId);
                    }
                    list.add(criteriaBuilder.and(in));
                }
                Predicate cond= null;
                if("初中".equals(schoolType)){
                    cond= criteriaBuilder.like(root.get("gradeName"),"%初%");
                }else if("高中".equals(schoolType)){
                    cond= criteriaBuilder.like(root.get("gradeName"),"%高%");
                }else {
                    cond= criteriaBuilder.like(root.get("gradeName"),"%年级%");
                }
                list.add(cond);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<HomeworkPublish> homeworkPublishList=homeworkPublishRepository.findAll(specification);
        educHomeworkData.setStudentNum(studentNum);
        educHomeworkData.setHomeworkNum(homeworkPublishList.size());
        Specification<StudentsHomeworkNew> stuSpecification = new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(!schoolIdList.isEmpty()){
                    CriteriaBuilder.In<Object> in = criteriaBuilder.in(root.get("schoolId"));
                    for(Long schoolId:schoolIdList){
                        in.value(schoolId);
                    }
                    list.add(criteriaBuilder.and(in));
                }
                Predicate cond= null;
                if("初中".equals(schoolType)){
                    cond= criteriaBuilder.like(root.get("grade"),"%初%");
                }else if("高中".equals(schoolType)){
                    cond= criteriaBuilder.like(root.get("grade"),"%高%");
                }else {
                    cond= criteriaBuilder.like(root.get("grade"),"%年级%");
                }
                list.add(cond);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Sort sort = Sort.by(Sort.Direction.DESC,"submitTime");
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=studentsHomeworkNewService.findAllSimpleDTOBySpecification(stuSpecification);
        Integer homeworkNum=0;
        Long homeworkTime=0l;
        Map<Long,Integer> schoolHomeworkNumMap=new HashMap<>();
        Map<Long,Long> schoolHomeworkTimeMap=new HashMap<>();
        Map<String,Long> gradeDayTimeMap=new HashMap<>();
        Map<String,Integer> gradeDayNumMap=new HashMap<>();
        Map<String,Long> dayTimeMap=new HashMap<>();
        Map<String,Integer> dayNumMap=new HashMap<>();
        for(StudentsHomeworkSimpleDTO studentsHomework:studentsHomeworkList){
            if(studentsHomework.getSubmitTime()!=null){
                schoolHomeworkNumMap.merge(studentsHomework.getSchoolId(), 1, Integer::sum);
                Long time = null;
                if(studentsHomework.getStartTime()!=null) {
                    time = studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime();
                }else{
                    time = studentsHomework.getSubmitTime().getTime() - studentsHomework.getCreateTime().getTime();
                }
                //studentsHomework.setDuration(Double.valueOf(time/ 1000l / 60 ));
                schoolHomeworkTimeMap.merge(studentsHomework.getSchoolId(), time, Long::sum);
                homeworkNum++;
                homeworkTime += time;
                //年级
                String gradeDay = studentsHomework.getGrade()+":"+DateUtil.formatDate(studentsHomework.getSubmitTime());
                gradeDayTimeMap.merge(gradeDay, time, Long::sum);
                gradeDayNumMap.merge(gradeDay, 1, Integer::sum);
                String day = DateUtil.formatDate(studentsHomework.getSubmitTime());
                dayTimeMap.merge(day, time, Long::sum);
                dayNumMap.merge(day, 1, Integer::sum);
            }
        }
        Double averageDuration = 0.0d;
        if(homeworkNum!=0) {
            averageDuration = Double.valueOf(homeworkTime / 1000l / 60 / homeworkNum);
        }
        educHomeworkData.setHomeworkAverageDuration(averageDuration);
        if(schoolHomeworkNumList==null||schoolHomeworkNumList.isEmpty()){
            schoolHomeworkNumList = new ArrayList<>();
        }else {
            for (SchoolHomeworkNum schoolHomeworkNum : schoolHomeworkNumList) {
                schoolHomeworkNum.setHomeworkNum(schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId()));
                if(schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId())!=null&&schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId())!=0) {
                    schoolHomeworkNum.setHomeworkAverageDuration(Double.valueOf(schoolHomeworkTimeMap.get(schoolHomeworkNum.getSchoolId()) / 1000l / 60 / schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId())));
                }else {
                    schoolHomeworkNum.setHomeworkAverageDuration(0.0d);
                }
            }
        }
        educHomeworkData.setSchoolHomeworkNumList(schoolHomeworkNumList);
        List<DurationStatistics>  durationStatisticsList = new ArrayList<>();
        for(String gradeDay:gradeDayTimeMap.keySet()){
            String grade = gradeDay.split(":")[0];
            String day = gradeDay.split(":")[1];

            DurationStatistics durationStatistics= new DurationStatistics();
            durationStatistics.setGrade(grade);
            durationStatistics.setDay(day);
            if(gradeDayNumMap.get(gradeDay)!=0){
                durationStatistics.setDuration(Double.valueOf(gradeDayTimeMap.get(gradeDay)/gradeDayNumMap.get(gradeDay)/1000/60));
            }
            durationStatisticsList.add(durationStatistics);
        }
        educHomeworkData.setDurationStatisticsList(durationStatisticsList);
        int maxNum = studentsHomeworkList.size()>10?10:studentsHomeworkList.size();
        List<StudentsHomeworkReport> reportList = new ArrayList<>();
        int n = 0;
        for(int i=0;i<studentsHomeworkList.size()&&n<maxNum;i++){
            StudentsHomeworkSimpleDTO homeworkSimpleDTO = studentsHomeworkList.get(i);
            StudentsHomeworkReport report =new StudentsHomeworkReport();
            BeanUtils.copyProperties(homeworkSimpleDTO,report);

            Double duration =0.0;
            if(homeworkSimpleDTO.getSubmitTime()!=null){
                duration  = BigDecimal.valueOf(homeworkSimpleDTO.getSubmitTime().getTime()-homeworkSimpleDTO.getCreateTime().getTime())
                        .divide(BigDecimal.valueOf(1000*60),2,BigDecimal.ROUND_HALF_UP).doubleValue();
                report.setDuration(duration);
                reportList.add(report);
                n++;
            }

        }
        educHomeworkData.setStudentsHomeworkNewList(reportList);
        Map<String,Double> dayAverageDuration =  new HashMap<>();
        for (String day:dayTimeMap.keySet()) {
            if(dayTimeMap.get(day)!=0) {
                dayAverageDuration.put(day,Double.valueOf(dayTimeMap.get(day)/dayNumMap.get(day)/1000/60));
            }
        }
        educHomeworkData.setDayAverageDuration(dayAverageDuration);
        return educHomeworkData;
    }

    @Override
    public HomeworkStatisticsDto getHomeworkStatistics(String startDate, String endDate) {
        HomeworkStatisticsDto statisticsDto = new HomeworkStatisticsDto();

        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                try {
                    Predicate condition = null;
                    if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
                        // 使用 DateUtil 获取日期范围
                        Date[] dateRange = DateUtil.getDateRange(startDate, endDate);
                        condition = criteriaBuilder.between(root.<Date>get("createTime"), dateRange[0], dateRange[1]);
                    } else {
                        condition = criteriaBuilder.conjunction();
                    }
                    Predicate condition1 = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    return criteriaBuilder.and(condition, condition1);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=studentsHomeworkNewService.findAllSimpleDTOBySpecification(specification);
        if(studentsHomeworkList==null||studentsHomeworkList.size()<=0){
            return statisticsDto;
        }
        Integer homeworkNum=studentsHomeworkList.size();
        statisticsDto.setHomeworkNum(homeworkNum);
        Integer submitNum=0;
        Integer auditNum = 0;
        Double totalTime=0.0;
        Double totalAccuracy=0.0;

        Map<String,Double> gradeDayAccuracyMap = new HashMap<>();
        Map<String,Integer> gradeDayNum= new HashMap<>();
        Map<String,Integer> subjectTimeNumMap = new HashMap<>();
        Map<String,Integer> gradeDayAuditNum= new HashMap<>();
        Map<String,Integer> gradeDaySubmitNum= new HashMap<>();
        Map<String,Integer> subjectNumMap = new HashMap<>();
        for (StudentsHomeworkSimpleDTO homework : studentsHomeworkList) {
            Double time = 0.0;
            if(homework.getSubmitTime()!=null){
                submitNum++;
                if(homework.getStartTime()!=null&&homework.getStartTime().before(homework.getSubmitTime())){
                    time =Double.valueOf(homework.getSubmitTime().getTime()- homework.getStartTime().getTime())/1000/60;

                }else if(homework.getCreateTime()!=null){
                    time =Double.valueOf(homework.getSubmitTime().getTime()- homework.getCreateTime().getTime())/1000/60;
                }
                if(time>60.0){
                    time = 60.0;
                }
                String gradeDaySubmit = homework.getGrade()+":"+DateUtil.formatDate(homework.getSubmitTime());
                gradeDaySubmitNum.merge(gradeDaySubmit, 1, Integer::sum);
            }
            if(homework.getAuditTime()!=null){
                auditNum ++;
            }
            totalTime += time;
            String gradeday = homework.getGrade()+":"+DateUtil.formatDate(homework.getCreateTime());
            //前期为了有数据，准确度设置为0
            if(homework.getAccuracy()==null){
                homework.setAccuracy(0.0);
            }
            if (homework.getAccuracy()!=null){
                totalAccuracy += homework.getAccuracy();
                gradeDayAccuracyMap.merge(gradeday, homework.getAccuracy(), Double::sum);
                gradeDayNum.merge(gradeday, 1, Integer::sum);
            }
            if(time!=0) {
                String key;
                if (time > 0.0 && time <= 10.0) {
                    key = homework.getSubject() + ":10";

                } else if (time > 10.0 && time <= 20.0) {
                    key = homework.getSubject() + ":20";
                } else if (time > 20.0 && time <= 30.0) {
                    key = homework.getSubject() + ":30";
                } else if (time > 30.0 && time <= 40.0) {
                    key = homework.getSubject() + ":40";
                } else {
                    key = homework.getSubject() + ":50";
                }
                subjectTimeNumMap.merge(key, 1, Integer::sum);
            }

            if(homework.getAuditTime()!=null){
                String gradeDayAudit = homework.getGrade()+":"+DateUtil.formatDate(homework.getAuditTime());
                gradeDayAuditNum.merge(gradeDayAudit, 1, Integer::sum);
            }
            if(StringUtils.isNotEmpty(homework.getSubject())) {
                subjectNumMap.merge(homework.getSubject(), 1, Integer::sum);
            }

        }
        BigDecimal compleRate = BigDecimal.ZERO;
        if(submitNum!=0&&homeworkNum!=0) {
            compleRate = BigDecimal.valueOf(submitNum).divide(BigDecimal.valueOf(homeworkNum),4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        BigDecimal auditRate = BigDecimal.ZERO;
        if(auditNum!=0&&homeworkNum!=0) {
            auditRate = BigDecimal.valueOf(auditNum).divide(BigDecimal.valueOf(homeworkNum),4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        statisticsDto.setCompleRate(compleRate);
        statisticsDto.setAuditRate(auditRate);
        Double averageDuration = 0.0;
        if(submitNum!=0) {
            averageDuration = BigDecimal.valueOf(totalTime).divide(BigDecimal.valueOf(submitNum), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        statisticsDto.setAverageDuration(averageDuration);
        Double averageAccuracy = BigDecimal.valueOf(totalAccuracy).divide(BigDecimal.valueOf(homeworkNum),2,BigDecimal.ROUND_HALF_UP).doubleValue() ;
        statisticsDto.setAverageAccuracy(averageAccuracy);

        List<GradeDayAccuracy> gradeDayAccuracyList = new ArrayList<>();
        for(String gradeDay:gradeDayAccuracyMap.keySet()){
            GradeDayAccuracy gradeDayAccuracy = new GradeDayAccuracy();
            String grade = gradeDay.split(":")[0];
            String day = gradeDay.split(":")[1];
            gradeDayAccuracy.setGrade(grade);
            gradeDayAccuracy.setDay(day);
            if(gradeDayNum.get(gradeDay)!=0){
                Double accuracy = gradeDayAccuracyMap.get(gradeDay)/gradeDayNum.get(gradeDay);
                gradeDayAccuracy.setAccuracy(accuracy);
            }else{
                gradeDayAccuracy.setAccuracy(0.0);
            }
            gradeDayAccuracyList.add(gradeDayAccuracy);
        }
        statisticsDto.setGradeDayAccuracyList(gradeDayAccuracyList);
        /**
         * 作业散点图
         */
        List<SubjectTimeNum> subjectTimeNumList = new ArrayList<>();
        for (String key : subjectTimeNumMap.keySet()) {
            SubjectTimeNum  subjectTimeNum = new SubjectTimeNum();
            String subject = key.split(":")[0];
            String durStr=key.split(":")[1];
            Integer duration = Integer.parseInt(durStr);
            subjectTimeNum.setSubject(subject);
            subjectTimeNum.setDuration(duration);
            subjectTimeNum.setNumber(subjectTimeNumMap.get(key));
            subjectTimeNumList.add(subjectTimeNum);
        }
        statisticsDto.setSubjectTimeNumList(subjectTimeNumList);
        /**
         * 作业批阅比率
         */
        List<GradeAuditRate> gradeSubmitRateList =new ArrayList<>();
        for(String gradeDay:gradeDaySubmitNum.keySet()){
            GradeAuditRate gradeAuditRate =  new GradeAuditRate();
            String grade = gradeDay.split(":")[0];
            String day = gradeDay.split(":")[1];
            gradeAuditRate.setGrade(grade);
            gradeAuditRate.setDay(day);
            BigDecimal submitRate = BigDecimal.ZERO;
            if(gradeDaySubmitNum.containsKey(gradeDay)&&gradeDayNum.containsKey(gradeDay)) {
                submitRate = BigDecimal.valueOf(gradeDaySubmitNum.get(gradeDay)).divide(BigDecimal.valueOf(gradeDayNum.get(gradeDay)),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            gradeAuditRate.setAuditRate(submitRate);
            gradeSubmitRateList.add(gradeAuditRate);
        }
        statisticsDto.setGradeSubmitRateList(gradeSubmitRateList);
        /**
         * 作业批阅比率
         */
        List<GradeAuditRate> gradeAuditRateList =new ArrayList<>();
        for(String gradeDay:gradeDayAuditNum.keySet()){
            GradeAuditRate gradeAuditRate =  new GradeAuditRate();
            String grade = gradeDay.split(":")[0];
            String day = gradeDay.split(":")[1];
            gradeAuditRate.setGrade(grade);
            gradeAuditRate.setDay(day);
            BigDecimal auditRate1 = BigDecimal.ZERO;
            if(gradeDayAuditNum.containsKey(gradeDay)&&gradeDayNum.containsKey(gradeDay)) {
                auditRate1 = BigDecimal.valueOf(gradeDayAuditNum.get(gradeDay)).divide(BigDecimal.valueOf(gradeDayNum.get(gradeDay)),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            gradeAuditRate.setAuditRate(auditRate1);
            gradeAuditRateList.add(gradeAuditRate);
        }
        statisticsDto.setGradeAuditRateList(gradeAuditRateList);
        statisticsDto.setSubjectNumMap(subjectNumMap);
        return statisticsDto;
    }

    @Override
    public AccuracyDto getAverageAccuracyStatistics(String subject, Long classId, String startDate, String endDate) {
        AccuracyDto accuracyDto =new AccuracyDto();
        //根据班级id、科目和时间查询班级所有学生的平均正确率
        List<AverageAccuracyDto> averageAccuracyDtos =new  ArrayList<>() ;
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list =  new ArrayList<>();

                if(StringUtils.isNotEmpty(subject)){
                    Predicate condition1 = criteriaBuilder.equal(root.get("subject"), subject);
                    list.add(condition1);
                }

                if(classId!=null){
                    Predicate condition2 = criteriaBuilder.equal(root.get("classesId"), classId);
                    list.add(condition2);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();

                try {
                    Predicate condition3 = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition3 = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
                        list.add(condition3);
                    }

                    Predicate condition = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    list.add(condition);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkNewList=studentsHomeworkNewService.findAllSimpleDTOBySpecification(specification);

        //班级平均正确率统计
        Map<String,Double> averageAccuracyMap =new  HashMap<>();
        Map<String,Integer> studentNumMap =new HashMap<>();
        Map<Long,String> classNameMap =new HashMap<>();
        for(StudentsHomeworkSimpleDTO studentsHomeworkNew:studentsHomeworkNewList){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if(studentsHomeworkNew.getCreateTime()==null){
                continue;
            }
            classNameMap.put(studentsHomeworkNew.getClassesId(),studentsHomeworkNew.getClassesName());
            String pulishDate = sdf.format(studentsHomeworkNew.getCreateTime());
            String classKey = studentsHomeworkNew.getClassesId()+","+pulishDate;
            if(averageAccuracyMap.get(classKey)==null){
                if(studentsHomeworkNew.getAccuracy()!=null){
                    averageAccuracyMap.put(classKey,studentsHomeworkNew.getAccuracy());
                }else{
                    averageAccuracyMap.put(classKey,0d);
                }
            }else {
                if(studentsHomeworkNew.getAccuracy()!=null){
                    Double totalAccuracy = averageAccuracyMap.get(classKey)+studentsHomeworkNew.getAccuracy();
                    averageAccuracyMap.put(classKey,totalAccuracy);
                }
            }
            if(studentNumMap.get(classKey)==null){
                studentNumMap.put(classKey,1);
            }else {
                studentNumMap.put(classKey,studentNumMap.get(classKey)+1);
            }
        }
        for(String key:averageAccuracyMap.keySet()){
            AverageAccuracyDto averageAccuracyDto = new AverageAccuracyDto();
            Double averageAccuracy = 0.0;
            if(studentNumMap.get(key)!=null&&studentNumMap.get(key)!=0) {
                averageAccuracy = BigDecimal.valueOf(averageAccuracyMap.get(key)).divide(BigDecimal.valueOf(studentNumMap.get(key)), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

            }
            averageAccuracyDto.setAverageAccuracy(averageAccuracy);
            Long classesId = Long.valueOf(key.substring(0,key.indexOf(",")));
            averageAccuracyDto.setClassId(classesId);
            String publishDate = key.substring(key.indexOf(",")+1);
            averageAccuracyDto.setPublishDate(publishDate);
            averageAccuracyDto.setClassName(classNameMap.get(classesId));
            averageAccuracyDtos.add(averageAccuracyDto);
        }
        accuracyDto.setAverageAccuracyDtos(averageAccuracyDtos);
        //科目平均正确率统计
        List<SubjectAccuracyDto> subjectAccuracyDtoList =new ArrayList<>();
        Map<String,Double> subjectAverageAccuracyMap =new  HashMap<>();
        Map<String,Integer> studentNumMap1 =new HashMap<>();
        for(StudentsHomeworkSimpleDTO studentsHomeworkNew:studentsHomeworkNewList){

            String classSubjectKey = studentsHomeworkNew.getClassesId()+","+studentsHomeworkNew.getSubject();
            if(subjectAverageAccuracyMap.get(classSubjectKey)==null){
                if(studentsHomeworkNew.getAccuracy()!=null){
                    subjectAverageAccuracyMap.put(classSubjectKey,studentsHomeworkNew.getAccuracy());
                }else{
                    subjectAverageAccuracyMap.put(classSubjectKey,0d);
                }
            }else {
                if(studentsHomeworkNew.getAccuracy()!=null){
                    Double totalAccuracy = subjectAverageAccuracyMap.get(classSubjectKey)+studentsHomeworkNew.getAccuracy();
                    subjectAverageAccuracyMap.put(classSubjectKey,totalAccuracy);
                }
            }
            if(studentNumMap.get(classSubjectKey)==null){
                studentNumMap1.put(classSubjectKey,1);
            }else {
                studentNumMap1.put(classSubjectKey,studentNumMap1.get(classSubjectKey)+1);
            }
        }
        for(String key:subjectAverageAccuracyMap.keySet()){
            SubjectAccuracyDto subjectAccuracyDto = new SubjectAccuracyDto();
            Double averageAccuracy = subjectAverageAccuracyMap.get(key)/studentNumMap1.get(key);
            subjectAccuracyDto.setAverageAccuracy(averageAccuracy);
            Long classesId = Long.valueOf(key.substring(0,key.indexOf(",")));
            subjectAccuracyDto.setClassId(classesId);
            String subjectStr = key.substring(key.indexOf(",")+1);
            subjectAccuracyDto.setSubject(subjectStr);
            subjectAccuracyDto.setClassName(classNameMap.get(classesId));
            subjectAccuracyDtoList.add(subjectAccuracyDto);
        }
        accuracyDto.setSubjectAccuracyDtos(subjectAccuracyDtoList);
        return accuracyDto;
    }

    @Override
    public List<StudentChapterAccuracy> studentChapterStatistics(String subject, Long classId, String chapter) {

        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list =  new ArrayList<>();

                if(StringUtils.isNotEmpty(subject)){
                    Predicate con = criteriaBuilder.equal(root.get("subject"),subject);
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(chapter)){
                    Predicate con = criteriaBuilder.equal(root.get("chapter"),subject);
                    list.add(con);
                }
                if(classId!=null){
                    Predicate con = criteriaBuilder.equal(root.get("classId"),classId);
                    list.add(con);
                }
                Predicate condition = criteriaBuilder.isNotNull(root.get("submitStatus"));
                list.add(condition);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };

        List<StudentChapterAccuracy>  studentChapterAccuracyList = new ArrayList<>();
        List<StudentsHomeworkSimpleDTO> homeworkNewList =studentsHomeworkNewService.findAllSimpleDTOBySpecification(specification);
        if(homeworkNewList==null||homeworkNewList.isEmpty()){
            return  studentChapterAccuracyList;
        }

        Map<String,Double> studentAverageAccuracyMap =new  HashMap<>();
        Map<String,Integer> studentNumMap1 =new HashMap<>();
        Map<Long,String> classMap =new  HashMap<>();
        Map<Long,String> studentMap =new  HashMap<>();
        for(StudentsHomeworkSimpleDTO studentsHomework:homeworkNewList){
            String key = studentsHomework.getStudentId()+":"+studentsHomework.getChapter()+":"+studentsHomework.getClassesId();
            if(studentAverageAccuracyMap.containsKey(key)&&studentsHomework.getAccuracy()!=null){
                studentAverageAccuracyMap.put(key,studentAverageAccuracyMap.get(key)+studentsHomework.getAccuracy());
            }else if(studentAverageAccuracyMap.containsKey(key)&&studentsHomework.getAccuracy()==null){

            }else if(studentsHomework.getAccuracy()!=null){
                studentAverageAccuracyMap.put(key,studentsHomework.getAccuracy());

            }else {
                studentAverageAccuracyMap.put(key,0.0d);
            }
            if (studentNumMap1.containsKey(key)) {
                studentNumMap1.put(key,studentNumMap1.get(key)+1);
            }else {
                studentNumMap1.put(key,1);
            }
            classMap.put(studentsHomework.getClassesId(),studentsHomework.getClassesName());
            studentMap.put(studentsHomework.getStudentId(),studentsHomework.getStudentName());
        }
        for(String key:studentAverageAccuracyMap.keySet()){
            StudentChapterAccuracy studentChapterAccuracy = new StudentChapterAccuracy();
            Long studentId = Long.parseLong(key.split(":")[0]);
            String chapterStr = key.split(":")[1];
            Long classIds = Long.parseLong(key.split(":")[2]);
            studentChapterAccuracy.setStudentId(studentId);
            studentChapterAccuracy.setStudentName(studentMap.get(studentId));
            studentChapterAccuracy.setChapter(chapterStr);
            studentChapterAccuracy.setClassId(classIds);
            studentChapterAccuracy.setClassName(classMap.get(classIds));
            Double averageAccuracy = studentAverageAccuracyMap.get(key)/studentNumMap1.get(key);
            studentChapterAccuracy.setAccuracy(averageAccuracy);
            studentChapterAccuracyList.add(studentChapterAccuracy);
        }
        studentChapterAccuracyList.sort(Comparator.comparing(StudentChapterAccuracy::getChapter));
        return studentChapterAccuracyList;
    }

    @Override
    public List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracy(String subject, Long classId, String startDate, String endDate) {
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list =  new ArrayList<>();

                if(StringUtils.isNotEmpty(subject)){
                    Predicate condition1 = criteriaBuilder.equal(root.get("subject"), subject);
                    list.add(condition1);
                }

                if(classId!=null){
                    Predicate condition2 = criteriaBuilder.equal(root.get("classesId"), classId);
                    list.add(condition2);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();

                try {
                    Predicate condition3 = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date startDate1 = sdf.parse(startDate);
                        Date endDate1 = sdf.parse(endDate);
                        condition3 = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
                        list.add(condition3);
                    }

                    Predicate condition = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    list.add(condition);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=studentsHomeworkNewService.findAllSimpleDTOBySpecification(specification);
        List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracyList=new ArrayList<>();
        if(studentsHomeworkList==null||studentsHomeworkList.isEmpty()){
            return  chapterKnowledgeAccuracyList;
        }
        Map<String,Double> accuracyMap=new HashMap<>();
        Map<String,Integer> studentNumMap=new HashMap<>();

        for(StudentsHomeworkSimpleDTO studentsHomework:studentsHomeworkList){
            String key = studentsHomework.getChapter()+":"+studentsHomework.getKnowledgePoint()+" ";
            if(accuracyMap.containsKey(key)&&studentsHomework.getAccuracy()!=null){
                accuracyMap.put(key,accuracyMap.get(key)+studentsHomework.getAccuracy());
            }else if(accuracyMap.containsKey(key)&&studentsHomework.getAccuracy()==null){

            }else if(studentsHomework.getAccuracy()!=null){
                accuracyMap.put(key,studentsHomework.getAccuracy());
            }
            else{
                accuracyMap.put(key,0.0d);
            }
            if(studentNumMap.containsKey(key)){
                studentNumMap.put(key,studentNumMap.get(key)+1);
            }else {
                studentNumMap.put(key,1);
            }
        }
        for(String key:studentNumMap.keySet()){
            if(!key.contains(":")){
                continue;
            }
            String chapterStr = key.split(":")[0];
            String knowledgePointStr = key.split(":")[1].trim();
            Double accuracy = accuracyMap.get(key)/studentNumMap.get(key);
            ChapterKnowledgeAccuracy  chapterKnowledgeAccuracy = new ChapterKnowledgeAccuracy();
            chapterKnowledgeAccuracy.setChapter(chapterStr);
            chapterKnowledgeAccuracy.setKnowledgePoint(knowledgePointStr);
            chapterKnowledgeAccuracy.setAccuracy(accuracy);
            chapterKnowledgeAccuracyList.add(chapterKnowledgeAccuracy);
        }
        return chapterKnowledgeAccuracyList;
    }
}
