package com.jlm.homework.service.impl;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.School;
import com.jlm.homework.feign.SchoolFeginClient;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.service.ExerciseBookServer;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.UserService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StudentsHomeworkNewServiceImpl implements IStudentsHomeworkNewService {
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private StudentFeginClient studentFeginClient;
    @Autowired
    private SchoolFeginClient schoolFeginClient;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
    @Autowired
    private UserService userService;

    @Override
    public void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish) {
        if(!homeworkPublish.getClassId().isEmpty()){
            String subject = null;
            if(homeworkPublish.getExerciseBookId() != null){
                ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                subject =  exerciseBook.getSubject();
            }

            for(Long classId:homeworkPublish.getClassId()){
                if(classId==null){
                    continue;
                }
                try {
                    Long schoolId=homeworkPublish.getSchoolId();
                    Result<Student> result = studentFeginClient.getStudentList(1,100,schoolId,null,classId,"0");
                    if(result.getCode()!=200){
                        throw new RuntimeException(result.getMsg());
                    }
                    List<Student> studentList=result.getRows();
                    for(Student student:studentList){
                        StudentsHomeworkNew studentsHomework = new StudentsHomeworkNew();
                        studentsHomework.setClassesId(classId);
                        studentsHomework.setClassesName(student.getClassesName());
                        studentsHomework.setHomeworkType(2);
                        studentsHomework.setHomeworkPublishId(homeworkPublish.getId());
                        studentsHomework.setHomeworkPublishName(homeworkPublish.getHomeworkName());
                        studentsHomework.setSchoolId(student.getSchoolId());
                        studentsHomework.setStudentId(student.getStudentId());
                        studentsHomework.setStudentName(student.getStudentName());
                        studentsHomework.setStudentUuid(student.getLinkUuid());
                        studentsHomework.setCreateTime(new Date());
                        studentsHomework.setSubmitStatus(0);
                        studentsHomework.setAuditStatus("0");
                        studentsHomework.setSubject(subject);
                        studentsHomework.setTopicImagesStr(homeworkPublish.getTopicImagesStr());
                        studentsHomework.setDeadline(homeworkPublish.getDeadline());
                        studentsHomeworkNewRepository.save(studentsHomework);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();

            Specification<StudentsHomeworkNew> specification = new Specification<StudentsHomeworkNew>() {

                @Override
                public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    if (studentsHomeworkRequest != null) {
                        Predicate condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Calendar calendar = Calendar.getInstance();
                        Predicate condition1 = null;
                        if (StringUtils.isNotEmpty(studentsHomeworkRequest.getStudentName())) {
                            condition1 = criteriaBuilder.like(root.get("studentName").as(String.class), "%" + studentsHomeworkRequest.getStudentName() + "%");
                        }else {
                            condition1 = criteriaBuilder.conjunction();
                        }
                        Predicate condition2 = null;
                        if (studentsHomeworkRequest.getSubmitStatus() != null) {
                            condition2 = criteriaBuilder.equal(root.get("submitStatus").as(String.class), studentsHomeworkRequest.getSubmitStatus());
                        }else {
                            condition2 = criteriaBuilder.conjunction();
                        }
                        try {
                            Predicate condition3 = null;
                            if (StringUtils.isNotEmpty(studentsHomeworkRequest.getSubmitTime())) {
                                Date submitTime = sdf.parse(studentsHomeworkRequest.getSubmitTime());
                                calendar.setTime(submitTime);
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                                Date submitTime1 = calendar.getTime();
                                condition3 = criteriaBuilder.between(root.get("submitTime").as(Date.class), submitTime, submitTime1);

                            }else {
                                condition3 = criteriaBuilder.conjunction();
                            }
                            Predicate condition4 = null;
                            if (StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditTime())) {
                                Date auditTime = sdf.parse(studentsHomeworkRequest.getAuditTime());
                                calendar.setTime(auditTime);
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                                Date auditTime1 = calendar.getTime();
                                condition4 = criteriaBuilder.between(root.get("auditTime").as(Date.class), auditTime, auditTime1);
                            }else {
                                condition4 = criteriaBuilder.conjunction();
                            }
                            Predicate condition5 = null;
                            if (studentsHomeworkRequest.getAuditStatus() != null) {
                                condition5 = criteriaBuilder.equal(root.get("auditStatus"), studentsHomeworkRequest.getAuditStatus());
                            }else {
                                condition5 = criteriaBuilder.conjunction();
                            }

                            query.where(condition0, condition1, condition2, condition3, condition4, condition5);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    return null;
                }


            };
            List<StudentsHomeworkNew> studentsHomeworkList = studentsHomeworkNewRepository.findAll(specification);
            return studentsHomeworkList;
    }

    @Override
    public StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework) {
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);
        StudentsHomeworkNew newSerach=new StudentsHomeworkNew();
        newSerach.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
        newSerach.setAuditStatus("2");
        Example<StudentsHomeworkNew> example = Example.of(studentsHomework);
        long count =studentsHomeworkNewRepository.count(example);
        if(count==0){
            HomeworkPublish homeworkPublish=homeworkPublishRepository.getById(studentsHomework.getHomeworkPublishId());
            homeworkPublish.setAuditStatus(1);
            homeworkPublishRepository.save(homeworkPublish);
        }
        return studentsHomework;
    }

    @Override
    public Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum-1, pageSize,sort);
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                if(studentsHomeworkRequest!=null){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();
                    Predicate condition1 = null;
                    if(StringUtils.isNotEmpty(studentsHomeworkRequest.getStudentName())){
                        condition1 = criteriaBuilder.like(root.get("studentName").as(String.class), "%" + studentsHomeworkRequest.getStudentName() + "%");
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    Predicate condition2 = null;
                    if(studentsHomeworkRequest.getSubmitStatus()!=null){
                        condition2 =criteriaBuilder.equal(root.get("submitStatus").as(String.class), studentsHomeworkRequest.getSubmitStatus());
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    try {
                        Predicate condition3 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getSubmitTime())){
                            Date submitTime = sdf.parse(studentsHomeworkRequest.getSubmitTime());
                            calendar.setTime(submitTime);
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                            Date submitTime1 = calendar.getTime();
                            condition3 = criteriaBuilder.between(root.get("submitTime").as(Date.class),submitTime,submitTime1);

                        }else {
                            condition3 = criteriaBuilder.conjunction();
                        }
                        Predicate condition4 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditTime())){
                            Date auditTime=sdf.parse(studentsHomeworkRequest.getAuditTime());
                            calendar.setTime(auditTime);
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                            Date auditTime1 = calendar.getTime();
                            condition4 = criteriaBuilder.between(root.get("auditTime").as(Date.class),auditTime,auditTime1 );
                        }else {
                            condition4 = criteriaBuilder.conjunction();
                        }
                        Predicate condition5 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditStatus())){
                            condition5 =criteriaBuilder.equal(root.get("auditStatus"), studentsHomeworkRequest.getAuditStatus());
                        }else {
                            condition5 = criteriaBuilder.conjunction();
                        }

                        query.where(condition0,condition1,condition2,condition3,condition4,condition5);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }
                return null;
            }


        };
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(specification,pageable);
        return studentsHomeworkList;
    }

    @Override
    public List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject, Long classId, String startDate,String endDate) {

        Specification<HomeworkPublish> specification= new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                try {
                    Predicate condition0 = null;
                    if(StringUtils.isNotEmpty(subject)){
                        condition0 =criteriaBuilder.equal(root.get("subject"), subject);
                    }else {
                        condition0 = criteriaBuilder.conjunction();
                    }
                    Predicate condition1 = null;
                    if(classId!=null){
                        condition1 =criteriaBuilder.like(root.get("classIds"), "%"+classId+"%");
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();
                    Predicate condition2 = null;
                    if(StringUtils.isNotEmpty(startDate)){
                        Date start = null;

                        start = sdf.parse(startDate);

                        calendar.setTime(start);

                        Date end = sdf.parse(endDate);
                        condition2 = criteriaBuilder.between(root.<Date>get("publishTime"),start,end);
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    query.where(condition0,condition1,condition2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
        List<HomeworkPublish> homeworkPublishList=homeworkPublishRepository.findAll(specification);
        List<StudentsHomeworkNew> studentsHomeworkList = new ArrayList<>();
        for(HomeworkPublish homeworkPublish:homeworkPublishList){
            StudentsHomeworkNew  studentsHomeworkNew=new StudentsHomeworkNew();
            studentsHomeworkNew.setHomeworkPublishId(homeworkPublish.getId());
            studentsHomeworkNew.setClassesId(classId);
            studentsHomeworkNew.setSubject(subject);
            List<StudentsHomeworkNew> studentsHomeworkNewList = studentsHomeworkNewRepository.findAll(Example.of(studentsHomeworkNew));
            studentsHomeworkList.addAll(studentsHomeworkNewList);
        }

        return studentsHomeworkList;
    }

    @Override
    public Page<StudentsHomeworkNew> getStudentsHomeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        Sort sort = Sort.by(Sort.Direction.DESC,"createTime");
        Pageable pageable = PageRequest.of(pageNum-1, pageSize,sort);
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if(studentsHomework!=null){

                    Predicate condition0 = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolId());
                    Predicate condition1 = null;
                    if(StringUtils.isNotEmpty(studentsHomework.getAuditStatus())){
                        condition1 = criteriaBuilder.equal(root.get("auditStatus"), studentsHomework.getAuditStatus());
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    Predicate condition2 = null;
                    if(studentsHomework.getClassesId()!=null){
                        condition2 = criteriaBuilder.equal(root.get("classesId"), studentsHomework.getClassesId());
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    Predicate condition4 = null;
                    if(studentsHomework.getStudentId()!=null){
                        condition4 = criteriaBuilder.equal(root.get("studentId"), studentsHomework.getStudentId());
                    }else {
                        condition4 = criteriaBuilder.conjunction();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();

                    try {
                        Predicate condition3 = null;
                        if(studentsHomework.getCreateTime()!=null){
                            Date createTime = studentsHomework.getCreateTime();
                            calendar.setTime(createTime);
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                            Date createTime1 = calendar.getTime();
                            condition3 = criteriaBuilder.between(root.get("createTime").as(Date.class),createTime,createTime1);

                        }else {
                            condition3 = criteriaBuilder.conjunction();
                        }


                        query.where(condition0,condition1,condition2,condition3,condition4);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
                return null;
            }


        };
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(specification,pageable);
        return studentsHomeworkList;
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



                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkNew> studentsHomeworkNewList=studentsHomeworkNewRepository.findAll(specification);

        //班级平均正确率统计
        Map<String,Double> averageAccuracyMap =new  HashMap<>();
        Map<String,Integer> studentNumMap =new HashMap<>();
        Map<Long,String> classNameMap =new HashMap<>();
        for(StudentsHomeworkNew studentsHomeworkNew:studentsHomeworkNewList){
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
            Double averageAccuracy = averageAccuracyMap.get(key)/studentNumMap.get(key);
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
        for(StudentsHomeworkNew studentsHomeworkNew:studentsHomeworkNewList){

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
    public StudentsHomeworkNew getById(Long id) {
        return studentsHomeworkNewRepository.findById(id).get();
    }

    @Override
    public List<StudentChapterAccuracy> studentChapterStatistics(String subject, Long classId, String chapter) {
        StudentsHomeworkNew homework= new StudentsHomeworkNew();
        if(StringUtils.isNotEmpty(subject)){
            homework.setSubject(subject);
        }
        if(StringUtils.isNotEmpty(chapter)){
            homework.setChapter(chapter);
        }
        if(classId!=null){
            homework.setClassesId(classId);
        }


        List<StudentChapterAccuracy>  studentChapterAccuracyList = new ArrayList<>();
        List<StudentsHomeworkNew> homeworkNewList =studentsHomeworkNewRepository.findAll(Example.of(homework));
        if(homeworkNewList==null||homeworkNewList.isEmpty()){
            return  studentChapterAccuracyList;
        }

        Map<String,Double> studentAverageAccuracyMap =new  HashMap<>();
        Map<String,Integer> studentNumMap1 =new HashMap<>();
        Map<Long,String> classMap =new  HashMap<>();
        Map<Long,String> studentMap =new  HashMap<>();
        for(StudentsHomeworkNew studentsHomework:homeworkNewList){
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



                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(specification);
        List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracyList=new ArrayList<>();
        if(studentsHomeworkList==null||studentsHomeworkList.isEmpty()){
            return  chapterKnowledgeAccuracyList;
        }
        Map<String,Double> accuracyMap=new HashMap<>();
        Map<String,Integer> studentNumMap=new HashMap<>();

        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            String key = studentsHomework.getChapter()+":"+studentsHomework.getKnowledgePoint();
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
            String chapterStr = key.split(":")[0];
            String knowledgePointStr = key.split(":")[1];
            Double accuracy = accuracyMap.get(key)/studentNumMap.get(key);
            ChapterKnowledgeAccuracy  chapterKnowledgeAccuracy = new ChapterKnowledgeAccuracy();
            chapterKnowledgeAccuracy.setChapter(chapterStr);
            chapterKnowledgeAccuracy.setKnowledgePoint(knowledgePointStr);
            chapterKnowledgeAccuracy.setAccuracy(accuracy);
            chapterKnowledgeAccuracyList.add(chapterKnowledgeAccuracy);
        }
        return chapterKnowledgeAccuracyList;
    }

    @Override
    public void endStudentsHomework(HomeworkPublish homeworkPublish) {
        List<StudentsHomeworkNew> studentsHomeworkList = this.getByHomeworkPublishId(homeworkPublish.getId(),new StudentsHomeworkRequest());
        if(studentsHomeworkList==null||studentsHomeworkList.isEmpty()){
            return;
        }
        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            studentsHomework.setDeadline(new Date());
            studentsHomework.setSubmitStatus(2);
            studentsHomeworkNewRepository.save(studentsHomework);
        }
    }

    @Override
    public SchoolHomeworkData getSchoolHomeworkData(Long schoolId) {
        SchoolHomeworkData schoolHomeworkData= new SchoolHomeworkData();
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        homeworkNew.setSchoolId(schoolId);
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(Example.of(homeworkNew));
        if(studentsHomeworkList==null||studentsHomeworkList.isEmpty()){
            return schoolHomeworkData;
        }

        Integer totleNum=0;
        Integer submittedNum=0;
        Integer unsubmittedNum=0;
        Map<String,Integer> gradeSubmitMap=new HashMap<>();
        Map<String,Integer> gradeUnSubmitMap=new HashMap<>();
        Map<String,Integer> gradeTotalMap=new HashMap<>();
        Map<Integer,Integer> homeworkNumMap = new HashMap<>();
        Map<Integer,Integer> auditNumMap = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        Map<String,Integer> classTotalMap = new HashMap<>();
        Map<String,Integer> classSubmitMap = new HashMap<>();
        List<Long> publishHomeworkIdList=new ArrayList<>();

        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            totleNum++;
            if(gradeTotalMap.containsKey(studentsHomework.getGrade())){
                gradeTotalMap.put(studentsHomework.getGrade(),gradeTotalMap.get(studentsHomework.getGrade())+1);
            }else {
                gradeTotalMap.put(studentsHomework.getGrade(),1);
            }
            if(studentsHomework.getSubmitStatus()!=null&&1==studentsHomework.getSubmitStatus()){
                submittedNum++;
                if(gradeSubmitMap.containsKey(studentsHomework.getGrade())){
                    gradeSubmitMap.put(studentsHomework.getGrade(),gradeSubmitMap.get(studentsHomework.getGrade())+1);
                }else {
                    gradeSubmitMap.put(studentsHomework.getGrade(),1);
                }
            }else{
                unsubmittedNum++;
                if(gradeUnSubmitMap.containsKey(studentsHomework.getGrade())){
                    gradeUnSubmitMap.put(studentsHomework.getGrade(),gradeUnSubmitMap.get(studentsHomework.getGrade())+1);
                }else {
                    gradeUnSubmitMap.put(studentsHomework.getGrade(),1);
                }
            }
            if(studentsHomework.getSubmitTime()!=null){
                Integer m = Math.toIntExact((studentsHomework.getSubmitTime().getTime() - studentsHomework.getCreateTime().getTime()) / 1000 / 60);
                if(m<=10){
                    if(homeworkNumMap.containsKey(10)){
                        homeworkNumMap.put(10,homeworkNumMap.get(10)+1);
                    }else{
                        homeworkNumMap.put(10,1);
                    }
                }else if(m<=30){
                    if(homeworkNumMap.containsKey(30)){
                        homeworkNumMap.put(30,homeworkNumMap.get(30)+1);
                    }else{
                        homeworkNumMap.put(30,1);
                    }
                }else if(m<=60){
                    if(homeworkNumMap.containsKey(60)){
                        homeworkNumMap.put(60,homeworkNumMap.get(30)+1);
                    }else{
                        homeworkNumMap.put(60,1);
                    }
                }else if(m<=90){
                    if(homeworkNumMap.containsKey(90)){
                        homeworkNumMap.put(90,homeworkNumMap.get(90)+1);
                    }else{
                        homeworkNumMap.put(90,1);
                    }
                }else if(m<=120){
                    if(homeworkNumMap.containsKey(120)){
                        homeworkNumMap.put(120,homeworkNumMap.get(120)+1);
                    }else{
                        homeworkNumMap.put(120,1);
                    }
                }
            }
            if(studentsHomework.getSubmitTime()!=null&&studentsHomework.getAuditTime()!=null){
                Integer m = Math.toIntExact((studentsHomework.getAuditTime().getTime() - studentsHomework.getSubmitTime().getTime()) / 1000 / 60);
                if(m<=10){
                    if(auditNumMap.containsKey(10)){
                        auditNumMap.put(10,auditNumMap.get(10)+1);
                    }else{
                        auditNumMap.put(10,1);
                    }
                }else if(m<=30){
                    if(auditNumMap.containsKey(30)){
                        auditNumMap.put(30,auditNumMap.get(30)+1);
                    }else{
                        auditNumMap.put(30,1);
                    }
                }else if(m<=60){
                    if(auditNumMap.containsKey(60)){
                        auditNumMap.put(60,auditNumMap.get(30)+1);
                    }else{
                        auditNumMap.put(60,1);
                    }
                }else if(m<=90){
                    if(auditNumMap.containsKey(90)){
                        auditNumMap.put(90,auditNumMap.get(90)+1);
                    }else{
                        auditNumMap.put(90,1);
                    }
                }else if(m<=120){
                    if(auditNumMap.containsKey(120)){
                        auditNumMap.put(120,auditNumMap.get(120)+1);
                    }else{
                        auditNumMap.put(120,1);
                    }
                }

                //今日
                String createDate = sdf.format(studentsHomework.getCreateTime());
                String className = studentsHomework.getClassesName();
                if(today.equals(createDate)){
                    if(classTotalMap.containsKey(className)){
                        classTotalMap.put(className,classTotalMap.get(className) + 1);
                    }else {
                        classTotalMap.put(className,1);
                    }
                }
                if(studentsHomework.getSubmitTime()!=null){
                    String day = sdf.format(studentsHomework.getSubmitTime());
                    if(day.equals(today)){
                        if(classSubmitMap.containsKey(className)){
                            classSubmitMap.put(className,classSubmitMap.get(className)+1);
                        }else{
                            classSubmitMap.put(className,1);
                        }
                    }
                }

            }
            if(studentsHomework.getAuditTime()!=null){
                String auditDate = sdf.format(studentsHomework.getAuditTime());
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
        for(String grade:gradeTotalMap.keySet()){
            GradeHomeworkSubmit  gradeHomeworkSubmit = new GradeHomeworkSubmit();
            gradeHomeworkSubmit.setGrade(grade);
            Double gradeSubmitRate = BigDecimal.valueOf(gradeSubmitMap.get(grade)).divide(BigDecimal.valueOf(gradeTotalMap.get(grade)),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            gradeHomeworkSubmit.setSubmitRate(gradeSubmitRate);
            Double gradeUnsubmitRate = BigDecimal.valueOf(gradeUnSubmitMap.get(grade)).divide(BigDecimal.valueOf(gradeTotalMap.get(grade)),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
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
            homeworkSubmit.setSubmitNum(classSubmitMap.get(className));
            Double submitRate = BigDecimal.valueOf(classSubmitMap.get(className))
                    .divide(BigDecimal.valueOf(classTotalMap.get(className)),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            homeworkSubmit.setSubmitRate(submitRate);
            todayHomeworkSubmit.add(homeworkSubmit);
        }
        schoolHomeworkData.setTodayHomeworkSubmit(todayHomeworkSubmit);
        //今日审批
        Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(!publishHomeworkIdList.isEmpty()){
                    Predicate predicate = criteriaBuilder.in(root.get("id").in(publishHomeworkIdList));
                    list.add(predicate);
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<HomeworkPublish> publishList=homeworkPublishRepository.findAll(specification);
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
            if(map.containsKey(homeworkPublish1.getExerciseBookName())){
                map.put(homeworkPublish1.getExerciseBookName(),map.get(homeworkPublish1.getExerciseBookName())+1);
            }else {
                map.put(homeworkPublish1.getExerciseBookName(),1);
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
    public EducHomeworkData getEducHomeworkData(Long educOrgId) {
        EducHomeworkData educHomeworkData=new  EducHomeworkData();
        List<SysSchool> schoolList=schoolFeginClient.getInfoByEducOrg(educOrgId,null);
        List<Long> schoolIdList =schoolList.stream().map(SysSchool::getSchoolId).toList();
        educHomeworkData.setSchoolNum(schoolList.size());
        Integer studentNum=0;
        List<SchoolHomeworkNum> schoolHomeworkNumList = new ArrayList<>();
        for(SysSchool school:schoolList){
            SchoolHomeworkNum  schoolHomeworkNum=new SchoolHomeworkNum();
            schoolHomeworkNum.setSchoolId(school.getSchoolId());
            schoolHomeworkNum.setSchoolName(school.getSchoolName());

            Result<Student> result = studentFeginClient.getStudentList(1,100,school.getSchoolId(),null,null,"0");
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
                    Predicate predicate = criteriaBuilder.in(root.get("schoolId").in(schoolIdList));
                    list.add(predicate);
                }
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
                    Predicate predicate = criteriaBuilder.in(root.get("schoolId").in(schoolIdList));
                    list.add(predicate);
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Sort sort = Sort.by(Sort.Direction.DESC,"submitTime");
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(stuSpecification,sort);
        Integer homeworkNum=0;
        Long homeworkTime=0l;
        Map<Long,Integer> schoolHomeworkNumMap=new HashMap<>();
        Map<Long,Long> schoolHomeworkTimeMap=new HashMap<>();
        Map<String,Long> gradeDayTimeMap=new HashMap<>();
        Map<String,Integer> gradeDayNumMap=new HashMap<>();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        Map<String,Long> dayTimeMap=new HashMap<>();
        Map<String,Integer> dayNumMap=new HashMap<>();
        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            if(studentsHomework.getSubmitTime()!=null){
                if(schoolHomeworkNumMap.containsKey(studentsHomework.getSchoolId())){
                    schoolHomeworkNumMap.put(studentsHomework.getSchoolId(),schoolHomeworkNumMap.get(studentsHomework.getSchoolId())+1);
                }else {
                    schoolHomeworkNumMap.put(studentsHomework.getSchoolId(),1);
                }
                Long time=studentsHomework.getSubmitTime().getTime()-studentsHomework.getCreateTime().getTime();
                if(schoolHomeworkTimeMap.containsKey(studentsHomework.getSchoolId())){
                    schoolHomeworkTimeMap.put(studentsHomework.getSchoolId(),schoolHomeworkTimeMap.get(studentsHomework.getSchoolId())+time);
                }else {
                    schoolHomeworkTimeMap.put(studentsHomework.getSchoolId(),time);
                }
                homeworkNum++;
                homeworkTime += time;
                //年级
                String gradeDay = studentsHomework.getGrade()+":"+sdf.format(studentsHomework.getSubmitTime());
                if(gradeDayTimeMap.containsKey(gradeDay)){
                    gradeDayTimeMap.put(gradeDay,gradeDayTimeMap.get(gradeDay)+time);
                }else {
                    gradeDayTimeMap.put(gradeDay,time);
                }
                if(gradeDayNumMap.containsKey(gradeDay)){
                    gradeDayNumMap.put(gradeDay,gradeDayNumMap.get(gradeDay)+1);
                }else {
                    gradeDayNumMap.put(gradeDay,1);
                }
                String day = sdf.format(studentsHomework.getSubmitTime());
                if(dayTimeMap.containsKey(day)){
                    dayTimeMap.put(day,dayTimeMap.get(day)+time);
                }else {
                    dayTimeMap.put(day,time);
                }
                if(dayNumMap.containsKey(day)){
                    dayNumMap.put(day,dayNumMap.get(day)+1);
                }else {
                    dayNumMap.put(day,1);
                }
            }
        }

        Double averageDuration = Double.valueOf(homeworkTime/1000l/60/homeworkNum);
        educHomeworkData.setHomeworkAverageDuration(averageDuration);
        for(SchoolHomeworkNum schoolHomeworkNum:schoolHomeworkNumList) {
            schoolHomeworkNum.setHomeworkNum(schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId()));

            schoolHomeworkNum.setHomeworkAverageDuration(Double.valueOf(schoolHomeworkTimeMap.get(schoolHomeworkNum.getSchoolId())/1000l/60/schoolHomeworkNumMap.get(schoolHomeworkNum.getSchoolId())));
        }
        educHomeworkData.setSchoolHomeworkNumList(schoolHomeworkNumList);
        List<DurationStatistics>  durationStatisticsList = new ArrayList<>();
        for(String gradeDay:gradeDayTimeMap.keySet()){
            String grade = gradeDay.split(":")[0];
            String day = gradeDay.split(":")[1];

            DurationStatistics durationStatistics= new DurationStatistics();
            durationStatistics.setGrade(grade);
            durationStatistics.setDay(day);

            durationStatistics.setDuration(Double.valueOf(gradeDayTimeMap.get(gradeDay)/gradeDayNumMap.get(gradeDay)/1000/60));
            durationStatisticsList.add(durationStatistics);
        }
        educHomeworkData.setDurationStatisticsList(durationStatisticsList);
        educHomeworkData.setStudentsHomeworkNewList(studentsHomeworkList.subList(0,10));
        Map<String,Double> dayAverageDuration =  new HashMap<>();
        for (String day:dayTimeMap.keySet()) {
            dayAverageDuration.put(day,Double.valueOf(dayTimeMap.get(day)/dayNumMap.get(day)/1000/60));
        }
        educHomeworkData.setDayAverageDuration(dayAverageDuration);
        return educHomeworkData;
    }
}
