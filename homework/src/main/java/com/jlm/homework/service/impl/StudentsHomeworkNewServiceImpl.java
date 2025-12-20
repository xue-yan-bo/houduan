package com.jlm.homework.service.impl;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlm.agent.AIServ.ZhiPuAIAgent;
import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.agent.domain.TopicReportEnt;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.SchoolFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.*;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.hibernate.query.Order;
import org.springframework.ai.content.Media;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class StudentsHomeworkNewServiceImpl implements IStudentsHomeworkNewService {
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Resource
    private HomeworkPublishQuestionRepository homeworkPublishQuestionRepository;
    @Resource
    private AiConfigRepository aiConfigRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private SchoolFeignClient schoolFeignClient;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
    @Autowired
    private IUserService userService;
    @Resource
    private EntityManager entityManager;
    @Autowired
    private IHomeworkStudentWriteDataService homeworkStudentWriteDataService;

    @Autowired
    private IWrongTitleBookService wrongTitleBookService;

    @Resource
    private ExerciseBookQuestionRepository exerciseBookQuestionRepository;

    @Resource
    private StudentsHomeworkCorrectRepository studentsHomeworkCorrectRepository;

    @Autowired
    private IStudentFeedbackService studentFeedbackService;

    @Autowired
    private IWrongTitleWriteDataService wrongTitleWriteDataService;

    @Autowired
    private ZhipuAIConfig zhipuAIConfig;
    @Autowired
    private AIUtil aiUtil;
    @Autowired
    private IQuestionAnalysisService questionAnalysisService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StudentsHomeworkStatisticsRepository studentsHomeworkStatisticsRepository;
    @Autowired
    private IStudentAICallService aiCallService;


    /**
     * 使用Criteria API根据Specification查询StudentsHomeworkSimpleDTO列表
     * 与studentsHomeworkNewRepository.findAll(specification)功能相同，但返回DTO对象
     */
    public List<StudentsHomeworkSimpleDTO> findAllSimpleDTOBySpecification(Specification<StudentsHomeworkNew> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentsHomeworkSimpleDTO> cq = cb.createQuery(StudentsHomeworkSimpleDTO.class);
        Root<StudentsHomeworkNew> root = cq.from(StudentsHomeworkNew.class);

        // 构建投影查询，选择所有需要的字段
        cq.multiselect(
            root.get("id"),
            root.get("homeworkType"),
            root.get("homeworkPublishId"),
            root.get("homeworkPublishName"),
            root.get("combinationQuestionsId"),
            root.get("designId"),
            root.get("schoolId"),
            root.get("grade"),
            root.get("classesId"),
            root.get("classesName"),
            root.get("studentId"),
            root.get("studentName"),
            root.get("studentUuid"),
            root.get("submitFileUrl"),
            root.get("startTime"),
            root.get("submitStatus"),
            root.get("submitTime"),
            root.get("createTime"),
            root.get("auditStatus"),
            root.get("auditTime"),
            root.get("errorReason"),
            root.get("suggestion"),
            root.get("design_file_id"),
            root.get("teacherAuditSuggest"),
            root.get("teacherAuditLevel"),
            root.get("subject"),
            root.get("accuracy"),
            root.get("classRank"),
            root.get("deadline"),
            root.get("commentText"),
            root.get("chapter"),
            root.get("knowledgePoint"),
            root.get("emendStatus"),
            root.get("dailyPracticeld"),
            root.get("dailyPracticeName"),
            root.get("score")
        );

        // 将Specification转换为Predicate
        Predicate predicate = null;
        if (specification != null) {
            predicate = specification.toPredicate(root, cq, cb);
            if (predicate != null) {
                cq.where(predicate);


            }

            cq.orderBy(cb.desc(root.get("createTime")));
        }

        // 执行查询
        TypedQuery<StudentsHomeworkSimpleDTO> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    @Override
    public void updateByPublishId(Long homeworkPublishId,String homeworkName, String topicImagesStr, Date deadline, Long dailyPracticeld, String dailyPracticeName, String dailyPracticePreview,  String chapter, String knowledgePoint,Integer submitStatus) {
        studentsHomeworkNewRepository.updateByPublishId(homeworkPublishId,homeworkName,topicImagesStr,deadline,dailyPracticeld,dailyPracticeName,dailyPracticePreview,chapter,knowledgePoint,submitStatus);
    }

    @Override
    public void updateSubmietNull(Long homeworkPublishId) {
        studentsHomeworkNewRepository.updateSubmietNull(homeworkPublishId);
    }

    @Override
    public void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish) {
        if(!homeworkPublish.getClassId().isEmpty()){
            String subject = null;
            if(homeworkPublish.getExerciseBookId() != null){
                ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                subject =  exerciseBook.getSubject();
            }
            if(StringUtils.isEmpty(subject)){
                subject = homeworkPublish.getSubject();
            }
            Integer studentSum = 0;
            for(Long classId:homeworkPublish.getClassId()){
                if(classId==null){
                    continue;
                }
                try {
                    StudentsHomeworkNew search = new StudentsHomeworkNew();
                    search.setHomeworkPublishId(homeworkPublish.getId());
                    search.setClassesId(classId);
                    Long count=studentsHomeworkNewRepository.count(Example.of(search));
                    if(count>0){
                        continue;
                    }
                    Long schoolId=homeworkPublish.getSchoolId();
                    Result<Student> result = studentFeignClient.getStudentList(1,100,schoolId,null,classId,"0");
                    if(result.getCode()!=200){
                        throw new RuntimeException(result.getMsg());
                    }
                    List<Student> studentList=result.getRows();
                    studentSum += studentList.size();
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
                        studentsHomework.setAuditStatus(0);
                        studentsHomework.setSubject(subject);
                        studentsHomework.setKnowledgePoint(homeworkPublish.getKnowledgePoint());
                        studentsHomework.setTopicImagesStr(homeworkPublish.getTopicImagesStr());
                        studentsHomework.setDeadline(homeworkPublish.getDeadline());
                        studentsHomework.setGrade(student.getGradeName());
                        studentsHomework.setDailyPracticeld(homeworkPublish.getDailyPracticeld());
                        studentsHomework.setDailyPracticeName(homeworkPublish.getDailyPracticeName());
                        studentsHomework.setDailyPracticePreview(homeworkPublish.getDailyPracticePreview());
                        studentsHomework.setChapter(homeworkPublish.getChapter());
                        studentsHomeworkNewRepository.save(studentsHomework);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            homeworkPublish.setStudentSum(studentSum);
            homeworkPublishRepository.save(homeworkPublish);
        }
    }

    @Override
    public List<StudentsHomeworkSimpleDTO> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest) {
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
            List<StudentsHomeworkSimpleDTO> studentsHomeworkList = this.findAllSimpleDTOBySpecification(specification);
            return studentsHomeworkList;
    }

    @Override
    public StudentsHomeworkNew  update(StudentsHomeworkNew studentsHomework) {
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);
        HomeworkPublish homeworkPublish=homeworkPublishRepository.getById(studentsHomework.getHomeworkPublishId());
        StudentsHomeworkNew finalStudentsHomework = studentsHomework;
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition = criteriaBuilder.equal(root.get("homeworkPublishId"), finalStudentsHomework.getHomeworkPublishId());
                list.add(condition);
                Predicate condition1 = criteriaBuilder.le(root.get("auditStatus"),1);
                list.add(condition1);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        long count =studentsHomeworkNewRepository.count(specification);
        if(count==0){
            homeworkPublish.setAuditStatus(2);
            homeworkPublishRepository.save(homeworkPublish);
        }

        return studentsHomework;
    }

    @Override
    public Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        Sort sort = Sort.by(Sort.Direction.DESC, "submitTime");
        Pageable pageable = PageRequest.of(pageNum-1, pageSize,sort);
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition0 = null;
                if(homeworkPublishId!=null){
                    condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                }else{
                    condition0 = criteriaBuilder.conjunction();
                }

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
                        condition2 =criteriaBuilder.equal(root.get("submitStatus"), studentsHomeworkRequest.getSubmitStatus());
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
                        Predicate condition6 = criteriaBuilder.isNotNull(root.get("submitStatus"));

                        query.where(condition0,condition1,condition2,condition3,condition4,condition5,condition6);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }
                return null;
            }


        };
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(specification,pageable);
        if(studentsHomeworkList!=null&&studentsHomeworkList.getContent()!=null&&studentsHomeworkList.getContent().size()>0){
            for(StudentsHomeworkNew studentsHomework:studentsHomeworkList.getContent()){
                List<HomeworkStudentWriteData> writeDatas=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"1");
                studentsHomework.setStudentWriteDataList(writeDatas);
                List<HomeworkStudentWriteData> writeDatas2=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"2");
                studentsHomework.setStudentWriteDataList2(writeDatas2);
                StudentsHomeworkCorrect search = new  StudentsHomeworkCorrect();
                search.setStudentsHomeworkId(studentsHomework.getId());
                search.setType(1);
                List<StudentsHomeworkCorrect> correctList = studentsHomeworkCorrectRepository.findAll(Example.of(search));
                studentsHomework.setHomeworkCorrectList(correctList);
                search.setType(2);
                List<StudentsHomeworkCorrect> correctList2 = studentsHomeworkCorrectRepository.findAll(Example.of(search));
                studentsHomework.setHomeworkCorrectList2(correctList2);
            }
        }
        return studentsHomeworkList;
    }

    @Override
    public Page<StudentsHomeworkNew> getClassHomeworkStatistics(Integer pageNum, Integer pageSize,String subject, Long classId, String startDate,String endDate) {

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
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    Predicate condition2 = null;
                    if(StringUtils.isNotEmpty(startDate)){
                        Date start = null;

                        start = sdf.parse(startDate +" 00:00:00");

                        calendar.setTime(start);

                        Date end = sdf.parse(endDate +" 23:59:59");
                        condition2 = criteriaBuilder.between(root.<Date>get("publishTime"),start,end);
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    Predicate condition3 = criteriaBuilder.isNotNull(root.get("submitStatus"));

                    query.where(condition0,condition1,condition2,condition3);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
        Sort sort = Sort.by(Sort.Direction.DESC,"id");
        List<HomeworkPublish> homeworkPublishList=homeworkPublishRepository.findAll(specification,sort);
        List<StudentsHomeworkNew> studentsHomeworkList = new ArrayList<>();
        for(HomeworkPublish homeworkPublish:homeworkPublishList){
            StudentsHomeworkNew  studentsHomeworkNew=new StudentsHomeworkNew();
            studentsHomeworkNew.setHomeworkPublishId(homeworkPublish.getId());
            studentsHomeworkNew.setClassesId(classId);
            studentsHomeworkNew.setSubject(subject);
            Sort sort1 = Sort.by(Sort.Direction.DESC,"accuracy","createTime");
            List<StudentsHomeworkNew> studentsHomeworkNewList = studentsHomeworkNewRepository.findAll(Example.of(studentsHomeworkNew),sort1);
            studentsHomeworkList.addAll(studentsHomeworkNewList);
        }
        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNum);
        int end = pageNum*pageSize>studentsHomeworkList.size()?studentsHomeworkList.size():pageNum*pageSize;
        List<StudentsHomeworkNew> contect = studentsHomeworkList.subList((pageNum-1)*pageSize,end);
        Page<StudentsHomeworkNew> page = new PageImpl<StudentsHomeworkNew>(contect,pageable,studentsHomeworkList.size());
        return page;
    }

    @Override
    public Page<StudentsHomeworkNew> getStudentsHomeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        Specification<StudentsHomeworkNew> specification = new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if (studentsHomework != null) {

                    Predicate condition0 = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                    list.add(condition0);
                    Predicate condition1 = null;
                    if (studentsHomework.getAuditStatus()!=null) {
                        condition1 = criteriaBuilder.equal(root.get("auditStatus"), studentsHomework.getAuditStatus());
                        list.add(condition1);
                        if(studentsHomework.getAuditStatus()>=2){
                            Predicate cond = criteriaBuilder.isNotNull(root.get("auditTime"));
                            list.add(cond);
                        }
                    }
                    Predicate condition2 = null;
                    if (studentsHomework.getClassesId() != null) {
                        condition2 = criteriaBuilder.equal(root.get("classesId"), studentsHomework.getClassesId());
                        list.add(condition2);
                    }
                    Predicate condition4 = null;
                    if (studentsHomework.getStudentId() != null) {
                        condition4 = criteriaBuilder.equal(root.get("studentId"), studentsHomework.getStudentId());
                        list.add(condition4);
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();

                    try {
                        Predicate condition3 = null;
                        if (studentsHomework.getCreateTime() != null) {
                            Date createTime = studentsHomework.getCreateTime();
                            calendar.setTime(createTime);
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            Date createTime1 = calendar.getTime();
                            condition3 = criteriaBuilder.between(root.get("createTime").as(Date.class), createTime, createTime1);
                            list.add(condition3);
                        }
                        Predicate condition5 = null;
                        if (studentsHomework.getEmendStatus() != null) {
                            condition5 = criteriaBuilder.isNotEmpty(root.get("emendStatus"));
                            list.add(condition5);
                        }
                        Predicate condition6 = null;
                        if (StringUtils.isNotEmpty(studentsHomework.getKnowledgePoint())) {
                            condition6 = criteriaBuilder.like(root.get("knowledgePoint"), "%" + studentsHomework.getKnowledgePoint() + "%");
                            list.add(condition6);
                        }
                        Predicate statusNotNUll=criteriaBuilder.isNotNull(root.get("submitStatus"));
                        list.add(statusNotNUll);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Page<StudentsHomeworkNew> studentsHomeworkList = studentsHomeworkNewRepository.findAll(specification, pageable);
        if (studentsHomework.getAuditStatus()!=null&&studentsHomework.getAuditStatus()!=4 && studentsHomework.getAuditStatus()!=5){
            List<StudentsHomeworkNew> homeworkList = studentsHomeworkList.getContent();
            for (StudentsHomeworkNew homework : homeworkList) {
                List<HomeworkStudentWriteData> writeDatas = homeworkStudentWriteDataService.findByStudentRecordId(homework.getId(), "1");
                homework.setStudentWriteDataList(writeDatas);
                List<HomeworkStudentWriteData> writeDatas2 = homeworkStudentWriteDataService.findByStudentRecordId(homework.getId(), "2");
                homework.setStudentWriteDataList2(writeDatas2);
                StudentsHomeworkCorrect search = new StudentsHomeworkCorrect();
                search.setStudentsHomeworkId(homework.getId());
                search.setType(1);
                List<StudentsHomeworkCorrect> correctList = studentsHomeworkCorrectRepository.findAll(Example.of(search));
                homework.setHomeworkCorrectList(correctList);
                search.setType(2);
                List<StudentsHomeworkCorrect> correctList2 = studentsHomeworkCorrectRepository.findAll(Example.of(search));
                studentsHomework.setHomeworkCorrectList2(correctList2);
            }
        }
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

                    Predicate condition = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    list.add(condition);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }


        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkNewList=this.findAllSimpleDTOBySpecification(specification);

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
    public StudentsHomeworkNew getById(Long id) {
        StudentsHomeworkNew studentsHomework=studentsHomeworkNewRepository.findById(id).get();
        List<HomeworkStudentWriteData> writeDatas=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"1");
        studentsHomework.setStudentWriteDataList(writeDatas);
        List<HomeworkStudentWriteData> writeDatas2=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"2");
        studentsHomework.setStudentWriteDataList2(writeDatas2);
        StudentsHomeworkCorrect search = new  StudentsHomeworkCorrect();
        search.setStudentsHomeworkId(studentsHomework.getId());
        search.setType(1);
        List<StudentsHomeworkCorrect> correctList = studentsHomeworkCorrectRepository.findAll(Example.of(search));
        studentsHomework.setHomeworkCorrectList(correctList);
        search.setType(2);
        List<StudentsHomeworkCorrect> correctList2 = studentsHomeworkCorrectRepository.findAll(Example.of(search));
        studentsHomework.setHomeworkCorrectList2(correctList2);
        return studentsHomework;
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
        List<StudentsHomeworkSimpleDTO> homeworkNewList =this.findAllSimpleDTOBySpecification(specification);
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
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=this.findAllSimpleDTOBySpecification(specification);
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

    @Override
    public void endStudentsHomework(HomeworkPublish homeworkPublish) {
        studentsHomeworkNewRepository.updateDeadline(homeworkPublish.getId(),new Date(),2);
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
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=this.findAllSimpleDTOBySpecification(specification);
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        Map<Long,String> classMap = new HashMap<>();
        Map<String,Integer> classTotalMap = new HashMap<>();
        Map<String,Integer> classSubmitMap = new HashMap<>();
        List<Long> publishHomeworkIdList=new ArrayList<>();

        for(StudentsHomeworkSimpleDTO studentsHomework:studentsHomeworkList){
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
                Integer m;
                if(studentsHomework.getStartTime()!=null) {
                    m = Math.toIntExact((studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime()) / 1000 / 60);
                }else{
                    m = Math.toIntExact((studentsHomework.getSubmitTime().getTime() - studentsHomework.getCreateTime().getTime()) / 1000 / 60);
                }
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
                        auditNumMap.put(60,auditNumMap.get(60)+1);
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



            }
            //今日
            String createDate = sdf.format(studentsHomework.getCreateTime());
            String className = studentsHomework.getClassesName();
            if(!classMap.containsKey(studentsHomework.getClassesId())){
                classMap.put(studentsHomework.getClassesId(),className);
            }
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
        Map<String, Object> totalRightRateMap = studentsHomeworkStatisticsRepository.getRightRatetotal(schoolId);
        HomeworkRightRate totalRightRate = new HomeworkRightRate();
        if(totalRightRateMap!=null){
            // 安全地将Number转换为Integer
            totalRightRate.setTotalNum(((Number) totalRightRateMap.get("totalNum")).intValue());
            totalRightRate.setRightNum(((Number) totalRightRateMap.get("rightNum")).intValue());
            totalRightRate.setErrorNum(((Number) totalRightRateMap.get("errorNum")).intValue());
            Double rightRate = 0.0;
            if(totalRightRate.getTotalNum()!=null&&totalRightRate.getRightNum()!=null){
                rightRate =  BigDecimal.valueOf(totalRightRate.getRightNum()).divide(BigDecimal.valueOf(totalRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                totalRightRate.setRightRate(rightRate);
            }
            Double erroRate =0.0;
            if(totalRightRate.getTotalNum()!=null&&totalRightRate.getErrorNum()!=null){
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
                homeworkRightRate.setClassName(classMap.get(classId));
                homeworkRightRate.setGrade((String) rightRateMap.get("grade"));
                // 安全地将Number转换为Integer
                homeworkRightRate.setTotalNum(((Number) rightRateMap.get("totalNum")).intValue());
                homeworkRightRate.setRightNum(((Number) rightRateMap.get("rightNum")).intValue());
                homeworkRightRate.setErrorNum(((Number) rightRateMap.get("errorNum")).intValue());
                Double rightRate = 0.0;
                if(homeworkRightRate.getTotalNum()!=null&&homeworkRightRate.getRightNum()!=null){
                    rightRate =  BigDecimal.valueOf(homeworkRightRate.getRightNum()).divide(BigDecimal.valueOf(homeworkRightRate.getTotalNum()),4,BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    homeworkRightRate.setRightRate(rightRate);
                }
                Double erroRate = 0.0;
                if(homeworkRightRate.getTotalNum()!=null&&homeworkRightRate.getErrorNum()!=null){
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
            if (gradeTotalMap != null && gradeTotalMap.get(grade) != null
                &&gradeSubmitMap !=null &&gradeSubmitMap.containsKey(grade) &&gradeSubmitMap.get(grade) != null){
                gradeSubmitRate = BigDecimal.valueOf(gradeSubmitMap.get(grade)).divide(BigDecimal.valueOf(gradeTotalMap.get(grade)), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            gradeHomeworkSubmit.setSubmitRate(gradeSubmitRate);
            Double gradeUnsubmitRate = 0.0d;
            if (gradeTotalMap != null && gradeTotalMap.containsKey(grade)
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
            if(classSubmitMap.containsKey(className)) {
                homeworkSubmit.setSubmitNum(classSubmitMap.get(className));
            }else{
                homeworkSubmit.setSubmitNum(0);
            }
            Double submitRate = 0.0d;
            if(classSubmitMap.containsKey(className)&&classSubmitMap.get(className) != null){
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
        for(Long publishId:publishHomeworkIdList){
            HomeworkPublish homeworkPublish=homeworkPublishRepository.findById(publishId).get();
            publishList.add(homeworkPublish);
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
                if (map.containsKey(homeworkPublish1.getExerciseBookName())) {
                    map.put(homeworkPublish1.getExerciseBookName(), map.get(homeworkPublish1.getExerciseBookName()) + 1);
                } else {
                    map.put(homeworkPublish1.getExerciseBookName(), 1);
                }
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
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=this.findAllSimpleDTOBySpecification(stuSpecification);
        Integer homeworkNum=0;
        Long homeworkTime=0l;
        Map<Long,Integer> schoolHomeworkNumMap=new HashMap<>();
        Map<Long,Long> schoolHomeworkTimeMap=new HashMap<>();
        Map<String,Long> gradeDayTimeMap=new HashMap<>();
        Map<String,Integer> gradeDayNumMap=new HashMap<>();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        Map<String,Long> dayTimeMap=new HashMap<>();
        Map<String,Integer> dayNumMap=new HashMap<>();
        for(StudentsHomeworkSimpleDTO studentsHomework:studentsHomeworkList){
            if(studentsHomework.getSubmitTime()!=null){
                if(schoolHomeworkNumMap.containsKey(studentsHomework.getSchoolId())){
                    schoolHomeworkNumMap.put(studentsHomework.getSchoolId(),schoolHomeworkNumMap.get(studentsHomework.getSchoolId())+1);
                }else {
                    schoolHomeworkNumMap.put(studentsHomework.getSchoolId(),1);
                }
                Long time = null;
                if(studentsHomework.getStartTime()!=null) {
                    time = studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime();
                }else{
                    time = studentsHomework.getSubmitTime().getTime() - studentsHomework.getCreateTime().getTime();
                }
                //studentsHomework.setDuration(Double.valueOf(time/ 1000l / 60 ));
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                try {

                    Calendar calendar = Calendar.getInstance();
                    Predicate condition = null;
                    if(StringUtils.isNotEmpty(startDate)&&StringUtils.isNotEmpty(endDate)){
                        Date start = sdf.parse(startDate + " 00:00:00");
                        calendar.setTime(start);
                        Date end = sdf.parse(endDate + " 23:59:59");
                        condition = criteriaBuilder.between(root.<Date>get("createTime"),start,end);
                    }else {
                        condition = criteriaBuilder.conjunction();
                    }
                    Predicate condition1 = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    query.where(condition,condition1);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
        List<StudentsHomeworkSimpleDTO> studentsHomeworkList=this.findAllSimpleDTOBySpecification(specification);
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
                if(homework.getStartTime()!=null){
                    time =Double.valueOf(homework.getSubmitTime().getTime()- homework.getStartTime().getTime())/1000/60;

                }else if(homework.getCreateTime()!=null){
                    time =Double.valueOf(homework.getSubmitTime().getTime()- homework.getCreateTime().getTime())/1000/60;
                }
                if(time>60.0){
                    time = 60.0;
                }
                String gradeDaySubmit = homework.getGrade()+":"+sdf1.format(homework.getSubmitTime());
                if(gradeDaySubmitNum.containsKey(gradeDaySubmit)){
                    gradeDaySubmitNum.put(gradeDaySubmit,gradeDaySubmitNum.get(gradeDaySubmit)+1);
                }else{
                    gradeDaySubmitNum.put(gradeDaySubmit,1);
                }
            }
            if(homework.getAuditTime()!=null){
                auditNum ++;
            }
            totalTime += time;
            String gradeday = homework.getGrade()+":"+sdf1.format(homework.getCreateTime());
            //前期为了有数据，准确度设置为0
            if(homework.getAccuracy()==null){
                homework.setAccuracy(0.0);
            }
            if (homework.getAccuracy()!=null){
                totalAccuracy += homework.getAccuracy();
                if(gradeDayAccuracyMap.containsKey(gradeday)){
                    gradeDayAccuracyMap.put(gradeday,gradeDayAccuracyMap.get(gradeday)+homework.getAccuracy());
                }else{
                    gradeDayAccuracyMap.put(gradeday,homework.getAccuracy());
                }
                if(gradeDayNum.containsKey(gradeday)){
                    gradeDayNum.put(gradeday,gradeDayNum.get(gradeday)+1);
                }else {
                    gradeDayNum.put(gradeday,1);
                }
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
                if (subjectTimeNumMap.containsKey(key)) {
                    subjectTimeNumMap.put(key, subjectTimeNumMap.get(key) + 1);
                } else {
                    subjectTimeNumMap.put(key, 1);
                }
            }

            if(homework.getAuditTime()!=null){
                String gradeDayAudit = homework.getGrade()+":"+sdf1.format(homework.getAuditTime());
                if(gradeDayAuditNum.containsKey(gradeDayAudit)){
                    gradeDayAuditNum.put(gradeDayAudit,gradeDayAuditNum.get(gradeDayAudit)+1);
                }else{
                    gradeDayAuditNum.put(gradeDayAudit,1);
                }
            }
            if(StringUtils.isNotEmpty(homework.getSubject())) {
                if (subjectNumMap.containsKey(homework.getSubject())) {
                    subjectNumMap.put(homework.getSubject(), subjectNumMap.get(homework.getSubject()) + 1);
                } else {
                    subjectNumMap.put(homework.getSubject(), 1);
                }
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
    public List<HomeWork2Board> getHomeWork2Board(String subject, String date, Long studentId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {

                    if(StringUtils.isNotEmpty(subject)){
                        Predicate condition = criteriaBuilder.equal(root.get("subject").as(String.class),subject);
                        list.add(condition);
                    }
                    Calendar calendar = Calendar.getInstance();
                    if(StringUtils.isNotEmpty(date)){
                        Date start = sdf.parse(date);
                        calendar.setTime(start);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Date end = calendar.getTime();
                        Predicate condition1 = criteriaBuilder.between(root.<Date>get("createTime").as(Date.class),start,end);
                        list.add(condition1);
                    }

                    if(studentId!=null){
                        Predicate  condition2 = criteriaBuilder.equal(root.get("studentId").as(Long.class),studentId);
                        list.add(condition2);
                    }
                    Predicate cond1 = criteriaBuilder.isNotNull(root.get("submitStatus"));
                    list.add(cond1);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Sort sort = Sort.by(Sort.Direction.DESC,"id");
        List<StudentsHomeworkNew> homeworkList=studentsHomeworkNewRepository.findAll(specification,sort);
        List<HomeWork2Board> homeWork2Boards =  new ArrayList<>();
        for(StudentsHomeworkNew homework:homeworkList){
            HomeWork2Board homeWork2Board = new HomeWork2Board();
            homeWork2Board.setHomeworkId(homework.getHomeworkPublishId());
            homeWork2Board.setHomeworkName(homework.getHomeworkPublishName());
            homeWork2Board.setSubject(homework.getSubject());
            if(homework.getTopicImages()!=null&&homework.getTopicImages().size()>0){
                homeWork2Board.setPageSize(homework.getTopicImages().size());
            }else{
                homeWork2Board.setPageSize(1);
            }

            homeWork2Boards.add(homeWork2Board);
        }
        return homeWork2Boards;
    }

    @Override
    public void saveWriteRecords(Long studentId,Long homeworkId, String type, Integer pageN, List<StudentsWriteRecord> studentsWriteRecords,Boolean isFinish) {
        StudentsHomeworkNew search = new StudentsHomeworkNew();
        search.setStudentId(studentId);
        search.setHomeworkPublishId(homeworkId);
        Optional<StudentsHomeworkNew> optional=studentsHomeworkNewRepository.findOne(Example.of(search));
        if(optional==null||optional.get()==null){
            return;
        }
        StudentsHomeworkNew studentsHomework=optional.get();
        HomeworkStudentWriteData writeData = new HomeworkStudentWriteData();
        writeData.setStudentHomeworkId(studentsHomework.getId());
        writeData.setPageNum(pageN);
        writeData.setStudentName(studentsHomework.getStudentName());
        writeData.setStudentId(studentId);
        writeData.setStudentsWriteRecords(studentsWriteRecords);
        writeData.setCreateTime(new Date());
        if(StringUtils.isNotEmpty(type)) {
            writeData.setType(type);
        }
        homeworkStudentWriteDataService.save(writeData);

        studentsHomework.setSubmitStatus(1);
        studentsHomework.setSubmitTime(new Date());
        if("1".equals(type)) {
            studentsHomework.setAuditStatus(1);
        }else if("2".equals(type)) {
            studentsHomework.setEmendStatus(2);
            studentsHomework.setAuditStatus(4);
        }

        studentsHomeworkNewRepository.save(studentsHomework);

        HomeworkPublish homeworkPublish=homeworkPublishRepository.findById(studentsHomework.getHomeworkPublishId()).get();
        homeworkPublish.setAuditStatus(1);
        homeworkPublishRepository.save(homeworkPublish);
        //异步处理AI智能审批
        if(isFinish){
            FutureTask<String> futureTask = new FutureTask<>(() -> {
                if("1".equals(type)){
                    aIauditStruc(studentsHomework.getId());
                }else {
                    String auditImages = "";
                    List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
                    //异步处理AI智能审批
                    //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
                    //QianWenAIUtil util = new QianWenAIUtil();
                    AIUtil util  = aiUtil.getAIUtil();
                    if("qianwen".equals(util.getAiName())){
                        util = (QianWenAIUtil)  util;
                    }else {
                        util = (ZhipuAIImageAnalysisUtil) util;
                    }
                    List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), type);
                    if (studentsHomework.getTopicImages() != null && studentsHomework.getTopicImages().size() > 0
                            && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
                        try {
                            List<String> imageNames = new ArrayList<>();
                            for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
                                String imageUrl = studentsHomework.getTopicImages().get(i);


                                for (HomeworkStudentWriteData writeData1 : homeworkStudentWriteDataList) {
                                    if (writeData1.getPageNum() == (i + 1)) {
                                        List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                                        BufferedImage resultImage = null;

                                        resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);

                                        // 保存结果图片
                                        String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                                        CoordinateImageGenerator.saveImage(resultImage, imageName);
                                        imageNames.add(imageName);
                                    }
                                }
                            }
                            Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                            //Map<String, String> resltMap = util.analyzeImageToJson(imageNames,"");
                            for (String key : resltMap.keySet()) {
                                String titleImage = resltMap.get(key);
                                if (titleImage.contains("<|begin_of_box|>")) {
                                    titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                                }
                                if (titleImage.contains("<|end_of_box|>")) {
                                    titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                                }
                                List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                                bigDtoAll.addAll(bigDtoList);
                                auditImages = auditImages + titleImage;
                                File imageFile = new File(key);
                                imageFile.delete();
                            }

                            System.out.println("批阅结果: " + auditImages);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                        try {
                            String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                            DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                            //试题识别

                            String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
                            String titleImage = util.analyzeImage(outputPath,prompt);
                            if (titleImage.contains("<|begin_of_box|>")) {
                                titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                            }
                            if (titleImage.contains("<|end_of_box|>")) {
                                titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                            }
                            System.out.println("批阅结果: " + titleImage);
                            List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                            auditImages = auditImages + "\n" + titleImage;
                            File imageFile = new File(outputPath);
                            imageFile.delete();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if ("1".equals(type)) {
                        studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoAll));
                    } else if ("2".equals(type)) {
                        studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
                    }
                    studentsHomeworkNewRepository.save(studentsHomework);
                }
                return "异步-OK";


            });
            Thread thread = new Thread(futureTask);
            thread.start();

        }
    }

    @Override
    public void saveStartTime(Long homeworkId) {
        if(homeworkId==null){
            return;
        }
        studentsHomeworkNewRepository.saveStartTime(homeworkId,new Date());
        /*Optional<StudentsHomeworkNew> optional=studentsHomeworkNewRepository.findById(homeworkId);
        if(optional!=null&&optional.isPresent()){
            StudentsHomeworkNew studentsHomework = optional.get();
            if(studentsHomework!=null) {
                studentsHomework.setStartTime(new Date());
                studentsHomeworkNewRepository.save(studentsHomework);
            }
        }*/
    }

    @Override
    public void emend(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework=studentsHomeworkNewRepository.findById(studentsHomeworkId).get();
        studentsHomework.setEmendStatus(1);
        studentsHomework.setAuditStatus(3);
        studentsHomeworkNewRepository.save(studentsHomework);
    }

    @Override
    public List<HomeWork2Board> getEmendHomeWork2Board(String subject, Long studentId) {
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {

                    if(StringUtils.isNotEmpty(subject)){
                        Predicate condition = criteriaBuilder.equal(root.get("subject").as(String.class),subject);
                        list.add(condition);
                    }
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.add(Calendar.DAY_OF_MONTH, -3);
                    Date start = calendar.getTime();


                    Date end = new Date();
                    Predicate condition1 = criteriaBuilder.between(root.<Date>get("createTime").as(Date.class),start,end);
                    list.add(condition1);


                    if(studentId!=null){
                        Predicate  condition2 = criteriaBuilder.equal(root.get("studentId").as(Long.class),studentId);
                        list.add(condition2);
                    }
                    Predicate  condition3 = criteriaBuilder.equal(root.get("emendStatus"),"1");
                    list.add(condition3);
                } catch (RuntimeException e) {
                    throw new RuntimeException(e);
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        Sort sort = Sort.by(Sort.Direction.DESC,"auditTime");
         List<StudentsHomeworkNew> homeworkList=studentsHomeworkNewRepository.findAll(specification,sort);
        List<HomeWork2Board> homeWork2Boards =  new ArrayList<>();
        for(StudentsHomeworkNew homework:homeworkList){
            HomeWork2Board homeWork2Board = new HomeWork2Board();
            homeWork2Board.setHomeworkId(homework.getHomeworkPublishId());
            homeWork2Board.setHomeworkName(homework.getHomeworkPublishName());
            homeWork2Board.setSubject(homework.getSubject());
            if(homework.getTopicImages()!=null&&homework.getTopicImages().size()>0){
                homeWork2Board.setPageSize(homework.getTopicImages().size());
            }else{
                homeWork2Board.setPageSize(1);
            }

            homeWork2Boards.add(homeWork2Board);
        }
        return homeWork2Boards;
    }

    @Override
    public StudentsHomeworkNew audit(StudentsHomeworkNew studentsHomework) {
        if(studentsHomework.getStudentWriteDataList()!=null){
            for (HomeworkStudentWriteData writeData:studentsHomework.getStudentWriteDataList()) {
                if(writeData.getOffset()!=null) {
                    homeworkStudentWriteDataService.updateOffset(writeData);
                }
            }
        }
        if(studentsHomework.getStudentWriteDataList2()!=null){
            for (HomeworkStudentWriteData writeData:studentsHomework.getStudentWriteDataList2()) {
                if(writeData.getOffset()!=null) {
                    homeworkStudentWriteDataService.updateOffset(writeData);
                }
            }
        }
        if(studentsHomework.getHomeworkCorrectList()!=null&&!studentsHomework.getHomeworkCorrectList().isEmpty()){
            for(StudentsHomeworkCorrect correct:studentsHomework.getHomeworkCorrectList()){
                correct.setStudentsHomeworkId(studentsHomework.getId());
                correct.setFileType("录音");
                correct.setCreaterType(1);
                correct.setType(1);
                correct.setCreateTime(new Date());
                studentsHomeworkCorrectRepository.save(correct);
            }

        }
        if(studentsHomework.getHomeworkCorrectList2()!=null&&!studentsHomework.getHomeworkCorrectList2().isEmpty()){
            for(StudentsHomeworkCorrect correct:studentsHomework.getHomeworkCorrectList2()){
                correct.setStudentsHomeworkId(studentsHomework.getId());
                correct.setFileType("录音");
                correct.setCreaterType(1);
                correct.setType(2);
                correct.setCreateTime(new Date());
                studentsHomeworkCorrectRepository.save(correct);
            }

        }
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);

        HomeworkPublish homeworkPublish=homeworkPublishRepository.getById(studentsHomework.getHomeworkPublishId());
        StudentsHomeworkNew finalStudent = studentsHomework;
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition = criteriaBuilder.equal(root.get("homeworkPublishId"), finalStudent.getHomeworkPublishId());
                list.add(condition);
                Predicate condition1 = criteriaBuilder.le(root.get("auditStatus"),1);
                list.add(condition1);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        long count =studentsHomeworkNewRepository.count(specification);
        if(count==0){

            homeworkPublish.setAuditStatus(2);
            homeworkPublishRepository.save(homeworkPublish);
        }
        try {
            StudentsHomeworkNew finalStudentsHomework = studentsHomework;
            FutureTask<String> futureTask = new FutureTask<>(() -> {
                // 异步执行的代码
                //根据老师审批标识和练习册分割题图片生成错题本
                if (finalStudentsHomework.getAuditLogoCoordinate() != null) {
                    List<AuditLogoCoordinate> auditLogoCoordinateList = finalStudentsHomework.getAuditLogoCoordinate();
                    ExerciseBookQuestion search = new ExerciseBookQuestion();
                    search.setExerciseBookId(homeworkPublish.getExerciseBookId());
                    List<ExerciseBookQuestion> bookQuestionList = exerciseBookQuestionRepository.findAll(Example.of(search));
                    for (AuditLogoCoordinate logoCoordinate : auditLogoCoordinateList) {
                        if ("X".equals(logoCoordinate.getSymbol())) {//错题
                            for (ExerciseBookQuestion question : bookQuestionList) {
                                QuestionCoordinate coordinates = question.getCoordinates();
                                if (logoCoordinate.getX() > coordinates.getX() && logoCoordinate.getX() < (coordinates.getX() + coordinates.getWidth())
                                        && logoCoordinate.getY() > (coordinates.getY() - coordinates.getHeight()) && logoCoordinate.getY() < coordinates.getY()) {
                                    WrongTitleBook wrongTitleBook = new WrongTitleBook();
                                    wrongTitleBook.setStudentsHomeworkId(finalStudentsHomework.getId());
                                    wrongTitleBook.setSource("学生作业：" + finalStudentsHomework.getHomeworkPublishName());
                                    wrongTitleBook.setQuestionId(question.getId());
                                    wrongTitleBook.setStudentId(finalStudentsHomework.getStudentId());
                                    wrongTitleBook.setStudentName(finalStudentsHomework.getStudentName());
                                    wrongTitleBook.setClassId(finalStudentsHomework.getClassesId());
                                    wrongTitleBook.setClassName(finalStudentsHomework.getClassesName());
                                    wrongTitleBook.setTitleImage(question.getCroppedUrl());
                                    wrongTitleBook.setSourceImageUrl(question.getSourceImageUrl());
                                    wrongTitleBook.setTitleBigNo(question.getTitleBigNo());
                                    wrongTitleBook.setTitleSmallNo(question.getTitleSmallNo());
                                    wrongTitleBookService.addWrongBook(wrongTitleBook);
                                }
                            }
                        }

                    }
                }
                //根据老师审批和练习册分割题图片生成错题本
                if (finalStudentsHomework.getAuditCoordinate() != null) {
                    List<AuditLogoCoordinate> auditCoordinateList = finalStudentsHomework.getAuditCoordinate();
                    ExerciseBookQuestion search = new ExerciseBookQuestion();
                    search.setExerciseBookId(homeworkPublish.getExerciseBookId());
                    List<ExerciseBookQuestion> bookQuestionList = exerciseBookQuestionRepository.findAll(Example.of(search));
                    for (ExerciseBookQuestion question : bookQuestionList) {
                        //把题内老师审批的起始点、结束点作为判断是否是错误符号的逻辑点
                        List<AuditLogoCoordinate> signList = new ArrayList<>();
                        for (AuditLogoCoordinate coordinate : auditCoordinateList) {
                            QuestionCoordinate questionCoordinates = question.getCoordinates();
                            if (coordinate.getX() > questionCoordinates.getX() && coordinate.getX() < (questionCoordinates.getX() + questionCoordinates.getWidth())
                                    && coordinate.getY() > (questionCoordinates.getY() - questionCoordinates.getHeight()) && coordinate.getY() < questionCoordinates.getY()
                                    && (Boolean.TRUE.equals(coordinate.getIsStart()) || Boolean.TRUE.equals(coordinate.getIsEnd()))
                            ) {
                                signList.add(coordinate);

                            }
                        }
                        if (PiontSignUtil.isCrossMark(signList)) {//如果是错误符号，加入错题本
                            WrongTitleBook wrongTitleBook = new WrongTitleBook();
                            wrongTitleBook.setStudentsHomeworkId(finalStudentsHomework.getId());
                            wrongTitleBook.setSource("学生作业：" + finalStudentsHomework.getHomeworkPublishName());
                            wrongTitleBook.setQuestionId(question.getId());
                            wrongTitleBook.setStudentId(finalStudentsHomework.getStudentId());
                            wrongTitleBook.setStudentName(finalStudentsHomework.getStudentName());
                            wrongTitleBook.setClassId(finalStudentsHomework.getClassesId());
                            wrongTitleBook.setClassName(finalStudentsHomework.getClassesName());
                            wrongTitleBook.setTitleImage(question.getCroppedUrl());
                            wrongTitleBook.setSourceImageUrl(question.getSourceImageUrl());
                            wrongTitleBook.setTitleBigNo(question.getTitleBigNo());
                            wrongTitleBook.setTitleSmallNo(question.getTitleSmallNo());
                            wrongTitleBookService.addWrongBook(wrongTitleBook);
                        }
                    }


                }

                return "异步生成错题本完成";
            });
            Thread thread = new Thread(futureTask);
            thread.start(); // 启动线程执行任务

            System.out.println(futureTask.get()); // 获取结果，会阻塞直到任务完成
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return studentsHomework;
    }

    @Override
    public StudentHomeworkDto homeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework) {
        StudentHomeworkDto studentHomeworkDto = new StudentHomeworkDto();
        Page<StudentsHomeworkSimpleDTO> homeworkPage  = this.getEmendPage(pageNum,pageSize,studentsHomework);
        studentHomeworkDto.setHomeworkPage(homeworkPage);
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if(studentsHomework!=null){

                    Predicate condition0 = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                    Predicate condition1 = null;
                    if(studentsHomework.getAuditStatus()!=null){
                        condition1 = criteriaBuilder.equal(root.get("auditStatus"), studentsHomework.getAuditStatus());
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    Predicate cond1 = null;
                    if(studentsHomework.getSubmitStatus()!=null){
                        cond1 = criteriaBuilder.equal(root.get("submitStatus"), studentsHomework.getSubmitStatus());
                    }else {
                        cond1 = criteriaBuilder.conjunction();
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
                        Long studentId=userService.getCurrentUserId();
                        if(studentId!=null&&1000L!=studentId){
                            condition4 = criteriaBuilder.equal(root.get("studentId"), studentId);
                        }else {
                            condition4 = criteriaBuilder.conjunction();
                        }
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
                        Predicate condition5 = null;
                        if(studentsHomework.getEmendStatus()!=null){
                            condition5 = criteriaBuilder.isNotEmpty(root.get("emendStatus"));
                        }else {
                            condition5 = criteriaBuilder.conjunction();
                        }
                        Predicate condition6 = criteriaBuilder.isNotNull(root.get("submitTime"));
                        Predicate statusNotNUll=criteriaBuilder.isNotNull(root.get("submitStatus"));

                        query.where(condition0,condition1,cond1,condition2,condition3,condition4,condition5,condition6,statusNotNUll);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
                return null;
            }


        };

        Long count=studentsHomeworkNewRepository.count(specification);

        Integer submitted=Integer.parseInt(count+"");
        Integer total=Integer.parseInt(homeworkPage.getTotalElements()+"");
        Integer unsubmitted =total-submitted;
        studentHomeworkDto.setTotal(total);
        studentHomeworkDto.setSubmitted(submitted);
        studentHomeworkDto.setUnsubmitted(unsubmitted);
        return studentHomeworkDto;
    }

    @Override
    public void saveFeedbackRecords(Long studentId,String subject, List<StudentsWriteRecord> studentsFeedbackRecords) {
        StudentFeedback feedback = new StudentFeedback();
        Date now  = new Date();
        feedback.setFeedbackContent(studentsFeedbackRecords);
        feedback.setFeedbackTime(now);
        feedback.setSubject(subject);
        feedback.setStudentId(studentId);
        ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
        if(resultDto!=null&&resultDto.getData()!=null){
            Student student  = resultDto.getData();
            feedback.setStudentName(student.getStudentName());
            feedback.setClassId(student.getClassesId());
            feedback.setClassName(student.getClassesName());
            log.info("学生信息：id"+student.getStudentId()+"姓名："+student.getStudentName());
        }
        feedback.setCreateTime(now);
        studentFeedbackService.save(feedback);
        log.info("--------完成反馈信息保存-------");
    }

    @Override
    public String aIaudit(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = this.getById(studentsHomeworkId);
        String auditImages = "";

        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        AIUtil util  = aiUtil.getAIUtil();
        if("qianwen".equals(util.getAiName())){
            util = (QianWenAIUtil)  util;
        }else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"1");
        List<QuestionAnalysis>  analyses = new ArrayList<>();
        List<String> imageNames = null;
        if(studentsHomework.getTopicImages()!=null&&studentsHomework.getTopicImages().size()>0
                &&!studentsHomework.getTopicImagesStr().endsWith(".docx")&&!studentsHomework.getTopicImagesStr().endsWith(".doc")){
            try {
                imageNames = new ArrayList<>();
                for(int i=0;i<studentsHomework.getTopicImages().size();i++) {
                    String imageUrl = studentsHomework.getTopicImages().get(i);


                    for(HomeworkStudentWriteData writeData1:homeworkStudentWriteDataList){
                        if(writeData1.getPageNum()==(i+1)&&StringUtils.isNotEmpty(imageUrl)) {
                            List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                            BufferedImage resultImage = null;

                            resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);

                            // 保存结果图片
                            String imageName = studentsHomework.getHomeworkPublishName()+"_"+studentsHomework.getStudentName()+"_"+writeData1.getPageNum()+"页作业.png";
                            CoordinateImageGenerator.saveImage(resultImage, imageName);
                            imageNames.add(imageName);

                        }
                    }
                }

                //analyses=util.batchReviewExamQuestions(imageNames);
                /*imageNames.stream().forEach(imageFile->{
                    File file = new File(imageFile);
                    file.delete();
                });*/
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if(StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
            try {
                String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);

                //试题识别
                //analyses=util.reviewExamQuestions(outputPath);
                imageNames = Arrays.asList(outputPath);
                /*File imageFile = new File(outputPath);
                imageFile.delete();*/
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl())) {
            imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl().split(","));
           /* try {
                analyses=util.batchReviewExamQuestions(imageNames);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }*/
        }
        Map<String,String> allmap = new HashMap<>();
        if(imageNames!=null&&imageNames.size()>0) {
            try {

                allmap=util.analyzeImagesAnswer(imageNames);
                log.info("AI分析结果"+allmap.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (String image : imageNames) {
                File imageFile = new File(image);
                imageFile.delete();

            }
        }
        Map<String, List<HomeworkAISmallDto>> aiResultMap = new HashMap<>();
        List<QuestionAnalysis> errorList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        List<QuestionAnalysis> zhuguanQuestions = new ArrayList<>();
        try {
            List<QuestionAnalysis> questionAnalysisList = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
            if(questionAnalysisList==null||questionAnalysisList.isEmpty()) {
                HomeworkPublishQuestion search = new HomeworkPublishQuestion();
                search.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                Sort sort = Sort.by(Sort.Direction.ASC,"id");
                List<HomeworkPublishQuestion> questionList = homeworkPublishQuestionRepository.findAll(Example.of(search));


                if (questionList != null && questionList.size() > 0) {

                    System.out.println(analyses.toString());

                    for (HomeworkPublishQuestion question : questionList) {
                        QuestionAnalysis questionAnalysis = new QuestionAnalysis();
                        BeanUtils.copyProperties(question, questionAnalysis);
                        questionAnalysis.setId(null);
                        if (StringUtils.isNotEmpty(questionAnalysis.getBigNumber()) && !map.containsKey(questionAnalysis.getBigNumber())) {
                            stringBuilder = stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
                            map.put(questionAnalysis.getBigNumber(), questionAnalysis.getQuestionType());
                        }
                        if (StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())) {
                            stringBuilder = stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
                        }
                        questionAnalysis.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                        questionAnalysis.setStudentsHomeworkId(studentsHomework.getId());
                        questionAnalysis.setClassesId(studentsHomework.getClassesId());
                        questionAnalysis.setStudentId(studentsHomework.getStudentId());
                        questionAnalysis.setStudentName(studentsHomework.getStudentName());
                        questionAnalysis.setGrade(studentsHomework.getGrade());
                        questionAnalysis.setSchoolId(studentsHomework.getSchoolId());
                        String titleNum = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getSmallNumber();
                        if (allmap.containsKey(titleNum)) {
                            questionAnalysis.setStudentAnswer(allmap.get(titleNum));
                        }
                        HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
                        smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
                        if(questionAnalysis.getStudentAnswer()==null||questionAnalysis.getStudentAnswer().contains("未作答")){
                            questionAnalysis.setIsCorrect(null);
                            stringBuilder = stringBuilder.append("未答题 ");
                            smallDto.setCorrectFlag("未答题");
                        }else {
                            // todo 有分析内容，直接按题目比较

                            if ("选择题".equals(questionAnalysis.getQuestionType())
                                    || "单选题".equals(questionAnalysis.getQuestionType())
                                    || "判断题".equals(questionAnalysis.getQuestionType())) {
                                if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer())
                                        && questionAnalysis.getStudentAnswer().equals(questionAnalysis.getReferenceAnswer())) {
                                    stringBuilder = stringBuilder.append("正确 ");
                                    smallDto.setCorrectFlag("正确");
                                } else if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer())) {
                                    stringBuilder = stringBuilder.append("错误 ");
                                    smallDto.setCorrectFlag("错误");
                                    questionAnalysis.setIsCorrect(false);
                                    errorList.add(questionAnalysis);
                                } else {
                                    stringBuilder = stringBuilder.append("未答题 ");
                                    smallDto.setCorrectFlag("未答题");
                                }
                            } else {
                                String pamt = "作为一个作业批阅助手，请批阅该题：" + questionAnalysis.getContent() + ",参考答案：" + questionAnalysis.getReferenceAnswer()
                                        + ",学生作答：" + questionAnalysis.getStudentAnswer() + ", 返回批阅结果，严格就判断正确与否";
                                String piyue = util.analyzeText(pamt);
                                if (piyue.contains("正确")) {
                                    questionAnalysis.setIsCorrect(true);
                                    stringBuilder = stringBuilder.append("正确 ");
                                    smallDto.setCorrectFlag("正确");
                                } else {
                                    questionAnalysis.setIsCorrect(false);
                                    stringBuilder = stringBuilder.append("错误 ");
                                    smallDto.setCorrectFlag("错误");
                                }
                            }

                        }
                        String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();
                        if (aiResultMap.containsKey(key)) {
                            List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
                            smallDtoList.add(smallDto);
                            aiResultMap.put(key, smallDtoList);
                        } else {
                            List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
                            smallDtoList.add(smallDto);
                            aiResultMap.put(key, smallDtoList);
                        }
                        questionAnalysisService.save(questionAnalysis);
                    }
                    auditImages = stringBuilder.toString();
                }
            }else{
                for(QuestionAnalysis questionAnalysis:questionAnalysisList){
                    String titleNum = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getSmallNumber();
                    if (allmap.containsKey(titleNum)) {
                        questionAnalysis.setStudentAnswer(allmap.get(titleNum));
                    }

                    HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
                    smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
                    if(questionAnalysis.getStudentAnswer()==null||
                            questionAnalysis.getStudentAnswer().contains("未作答")||
                            questionAnalysis.getStudentAnswer().contains("未答题")){
                        questionAnalysis.setIsCorrect(null);
                        stringBuilder = stringBuilder.append("未答题 ");
                        smallDto.setCorrectFlag("未答题");
                    }else {

                        // todo 题目判断

                        if ("选择题".equals(questionAnalysis.getQuestionType())
                                || "单选题".equals(questionAnalysis.getQuestionType())
                                || "判断题".equals(questionAnalysis.getQuestionType())) {
                            if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer())
                                    && questionAnalysis.getStudentAnswer().equals(questionAnalysis.getReferenceAnswer())) {
                                stringBuilder = stringBuilder.append("正确 ");
                                smallDto.setCorrectFlag("正确");
                                questionAnalysis.setIsCorrect(true);
                            } else if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer())) {
                                stringBuilder = stringBuilder.append("错误 ");
                                smallDto.setCorrectFlag("错误");
                                questionAnalysis.setIsCorrect(false);
                                errorList.add(questionAnalysis);
                            } else {
                                stringBuilder = stringBuilder.append("未答题 ");
                                smallDto.setCorrectFlag("未答题");
                            }
                        } else if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer())&&!"未作答".equals(questionAnalysis.getStudentAnswer())) {
                            String pamt = "作为一个作业批阅助手，请批阅该题：" + questionAnalysis.getContent() + ",参考答案：" + questionAnalysis.getReferenceAnswer()
                                    + ",学生作答：" + questionAnalysis.getStudentAnswer() + ", 返回批阅结果，严格就判断正确与否";
                            String piyue = util.analyzeText(pamt);
                            if (piyue.contains("正确")) {
                                questionAnalysis.setIsCorrect(true);
                                stringBuilder = stringBuilder.append("正确 ");
                                smallDto.setCorrectFlag("正确");
                            } else {
                                questionAnalysis.setIsCorrect(false);
                                stringBuilder = stringBuilder.append("错误 ");
                                smallDto.setCorrectFlag("错误");
                            }
                        } else {
                            stringBuilder = stringBuilder.append("未答题 ");
                            smallDto.setCorrectFlag("未答题");
                        }

                    }
                    String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();


                    if (aiResultMap.containsKey(key)) {
                        List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
                        smallDtoList.add(smallDto);
                        aiResultMap.put(key, smallDtoList);
                    } else {
                        List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
                        smallDtoList.add(smallDto);
                        aiResultMap.put(key, smallDtoList);
                    }
                    questionAnalysisService.save(questionAnalysis);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<HomeworkAIBigDto> bigDtoList = new ArrayList<>();
        for(String key:aiResultMap.keySet()){
            String bigNumber = key.split(":")[0];
            String questionType = key.split(":")[1];
            HomeworkAIBigDto bigDto = new HomeworkAIBigDto();
            bigDto.setBigNumber(bigNumber);
            bigDto.setQuestionType(questionType);
            bigDto.setSmallDtoList(aiResultMap.get(key));
            bigDtoList.add(bigDto);
        }
        studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoList));
        studentsHomeworkNewRepository.save(studentsHomework);
        if(errorList!=null&&errorList.size()>0){
            for(QuestionAnalysis questionAnalysis:errorList){
                WrongTitleBook wrongTitleBook = new WrongTitleBook();
                wrongTitleBook.setTitleBigNo(questionAnalysis.getBigNumber());
                wrongTitleBook.setTitleSmallNo(questionAnalysis.getSmallNumber());
                wrongTitleBook.setTitleContext(questionAnalysis.getContent());
                wrongTitleBook.setStudentAnswer(questionAnalysis.getStudentAnswer());
                wrongTitleBook.setParse(questionAnalysis.getAnalysis());
                wrongTitleBook.setKnowledgePoint(questionAnalysis.getKnowledgePoints());
                wrongTitleBook.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                wrongTitleBook.setStudentsHomeworkId(studentsHomeworkId);
                wrongTitleBook.setSource("学生作业：" + studentsHomework.getHomeworkPublishName());
                wrongTitleBook.setStudentId(studentsHomework.getStudentId());
                wrongTitleBook.setStudentName(studentsHomework.getStudentName());
                wrongTitleBook.setClassId(studentsHomework.getClassesId());
                wrongTitleBook.setClassName(studentsHomework.getClassesName());
                wrongTitleBookService.addWrongBook(wrongTitleBook);
            }
        }
        return auditImages;
    }

    public String aIauditStruc(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = this.getById(studentsHomeworkId);
        String auditImages = "";

        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        AIUtil util  = aiUtil.getAIUtil();
        if("qianwen".equals(util.getAiName())){
            util = (QianWenAIUtil)  util;
        }else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }

        List<HomeworkStudentWriteData> homeworkStudentWriteDataList=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"1");
        List<QuestionAnalysis>  analyses = new ArrayList<>();
        List<String> imageNames = null;
        if(studentsHomework.getTopicImages()!=null&&studentsHomework.getTopicImages().size()>0
                &&!studentsHomework.getTopicImagesStr().endsWith(".docx")&&!studentsHomework.getTopicImagesStr().endsWith(".doc")){
            try {
                imageNames = new ArrayList<>();
                for(int i=0;i<studentsHomework.getTopicImages().size();i++) {
                    String imageUrl = studentsHomework.getTopicImages().get(i);


                    for(HomeworkStudentWriteData writeData1:homeworkStudentWriteDataList){
                        if(writeData1.getPageNum()==(i+1)&&StringUtils.isNotEmpty(imageUrl)) {
                            List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                            BufferedImage resultImage = null;

                            resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);

                            // 保存结果图片
                            String imageName = studentsHomework.getHomeworkPublishName()+"_"+studentsHomework.getStudentName()+"_"+writeData1.getPageNum()+"页作业.png";
                            CoordinateImageGenerator.saveImage(resultImage, imageName);
                            imageNames.add(imageName);

                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if(StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
            try {
                String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);


                imageNames = Arrays.asList(outputPath);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl())) {
            imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl().split(","));

        }

        TopicReportEnt topicReport = null;


        if(imageNames!=null&&imageNames.size()>0) {
            try {

                // AI直接解析，一步出结果
                List<Media> medias = new ArrayList<Media>();

                for (String imagePath : imageNames) {
                    String base64Image = null;
                    try {
                        base64Image = AIFileUtil.encodeImageToBase64(imagePath);
                        Media media = Media.builder().mimeType(MediaType.IMAGE_PNG).data(base64Image)
                                .build();
                        medias.add(media);
                    } catch (IOException e) {
                        continue;
                    }
                }
                topicReport = aiCallService.obtainTeacherJudgeAnswerNoStruc("请分析所有试卷图片题目，结构化输出分析结果", medias);
                log.info("AI_STRUC获取解析结果, 分析结果"+topicReport.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (String image : imageNames) {
                File imageFile = new File(image);
                imageFile.delete();

            }
        }

        Map<String, List<HomeworkAISmallDto>> aiResultMap = new HashMap<>();
        List<QuestionAnalysis> errorList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();

//        if("qianwen".equals(util.getAiName())){
            // 千问，准确性高，结果直接入库
            List<SubQuestionsEnt> answers = topicReport.getAnswers();
            if(topicReport != null && CollectionUtils.isNotEmpty(topicReport.getAnswers())){

                // 清理库中解析答案
                List<QuestionAnalysis> listByStudHomeId = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
                if(CollectionUtils.isNotEmpty(listByStudHomeId)){
                    questionAnalysisService.deleteByStudHomeId(studentsHomeworkId);
                    /*listByStudHomeId.forEach(analysis->{
                        questionAnalysisService.deleteById(analysis.getId());
                    });*/
                }
                for(SubQuestionsEnt temp:answers) {

                    // 若库里没有试题和答案，则直接用AI阅卷，返回的内容出结果
                    QuestionAnalysis questionAnalysis = new QuestionAnalysis();
                    questionAnalysis.setId(null);
                    if (StringUtils.isNotEmpty(questionAnalysis.getBigNumber()) && !map.containsKey(questionAnalysis.getBigNumber())) {
                        stringBuilder = stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
                        map.put(questionAnalysis.getBigNumber(), questionAnalysis.getQuestionType());
                    }
                    if (StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())) {
                        stringBuilder = stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
                    }
                    questionAnalysis.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                    questionAnalysis.setStudentsHomeworkId(studentsHomework.getId());
                    questionAnalysis.setClassesId(studentsHomework.getClassesId());
                    questionAnalysis.setStudentId(studentsHomework.getStudentId());
                    questionAnalysis.setStudentName(studentsHomework.getStudentName());
                    questionAnalysis.setGrade(studentsHomework.getGrade());
                    questionAnalysis.setSchoolId(studentsHomework.getSchoolId());
                    questionAnalysis.setContent(temp.getQuestion_content());
                    questionAnalysis.setBigNumber(temp.getMajor_question_id());
                    questionAnalysis.setSmallNumber(temp.getQuestion_id());

                    List<String> answerText = temp.getAnswer_text();
                    questionAnalysis.setStudentAnswer(answerText != null ? String.join(",,,", answerText) : "");
                    questionAnalysis.setReferenceAnswer(temp.getCorrect_answer() != null ? String.join(",,,", temp.getCorrect_answer()) : "");

                    HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
                    smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
                    Boolean judgeRes = Boolean.valueOf(temp.getIs_correct());
                    if (judgeRes == null) {
                        questionAnalysis.setIsCorrect(null);
                        smallDto.setCorrectFlag("未答题");
                    } else {
                        questionAnalysis.setIsCorrect(judgeRes);
                        if (judgeRes) {
                            stringBuilder = stringBuilder.append("正确 ");
                            smallDto.setCorrectFlag("正确");
                        } else {
                            stringBuilder = stringBuilder.append("错误 ");
                            smallDto.setCorrectFlag("错误");
                        }
                    }
                    String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();
                    if (aiResultMap.containsKey(key)) {
                        List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
                        smallDtoList.add(smallDto);
                        aiResultMap.put(key, smallDtoList);
                    } else {
                        List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
                        smallDtoList.add(smallDto);
                        aiResultMap.put(key, smallDtoList);
                    }
                    questionAnalysisService.save(questionAnalysis);
                }
            }

//        }

//        else {
//
//
//            List<QuestionAnalysis> zhuguanQuestions = new ArrayList<>();
//            try {
//                List<QuestionAnalysis> questionAnalysisList = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
//                if (questionAnalysisList == null || questionAnalysisList.isEmpty()) {
//                    // 题目没有被分析过，取题目列表
//                    HomeworkPublishQuestion search = new HomeworkPublishQuestion();
//                    search.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
//                    Sort sort = Sort.by(Sort.Direction.ASC, "id");
//                    List<HomeworkPublishQuestion> questionList = homeworkPublishQuestionRepository.findAll(Example.of(search));
//
//                    // 1.若库里有试题和答案，则用AI提取学生答案，和库里的答案对比出结果 ，
//                    // 2.若库里没有试题和答案，则直接用AI阅卷，返回的内容出结果
//                    if (questionList != null && questionList.size() > 0) {
//
//                        System.out.println(analyses.toString());
//
//                        for (HomeworkPublishQuestion question : questionList) {
//                            QuestionAnalysis questionAnalysis = new QuestionAnalysis();
//                            BeanUtils.copyProperties(question, questionAnalysis);
//                            questionAnalysis.setId(null);
//                            if (StringUtils.isNotEmpty(questionAnalysis.getBigNumber()) && !map.containsKey(questionAnalysis.getBigNumber())) {
//                                stringBuilder = stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
//                                map.put(questionAnalysis.getBigNumber(), questionAnalysis.getQuestionType());
//                            }
//                            if (StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())) {
//                                stringBuilder = stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
//                            }
//                            questionAnalysis.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
//                            questionAnalysis.setStudentsHomeworkId(studentsHomework.getId());
//                            questionAnalysis.setClassesId(studentsHomework.getClassesId());
//                            questionAnalysis.setStudentId(studentsHomework.getStudentId());
//                            questionAnalysis.setStudentName(studentsHomework.getStudentName());
//                            questionAnalysis.setGrade(studentsHomework.getGrade());
//                            questionAnalysis.setSchoolId(studentsHomework.getSchoolId());
//                            String titleNum = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getSmallNumber();
//
//                            /* 获取学生答题内容 */
//                            SubQuestionsEnt stuWriteAnswer = getStuWriteAnswerNoStruc(topicReport.getAnswers(), questionAnalysis.getBigNumber(), questionAnalysis.getSmallNumber(), null);
//
//                            if (stuWriteAnswer != null) {
//                                List<String> answerText = stuWriteAnswer.getAnswer_text();
//                                questionAnalysis.setStudentAnswer(answerText != null ? String.join(",,,", answerText) : "");
//                                questionAnalysis.setReferenceAnswer(stuWriteAnswer.getCorrect_answer() != null ? String.join(",,,", stuWriteAnswer.getCorrect_answer()) : "");
//                            }
//                            HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
//                            smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
//                            if (questionAnalysis.getStudentAnswer() == null || questionAnalysis.getStudentAnswer().contains("未作答")) {
//                                questionAnalysis.setIsCorrect(null);
//                                stringBuilder = stringBuilder.append("未答题 ");
//                                smallDto.setCorrectFlag("未答题");
//                            } else {
//                                if ("选择题".equals(questionAnalysis.getQuestionType())
//                                        || "单选题".equals(questionAnalysis.getQuestionType())
//                                        || "判断题".equals(questionAnalysis.getQuestionType())) {
//
//                                    Boolean judgeRes = Boolean.valueOf(stuWriteAnswer.getIs_correct());
//                                    if (judgeRes == null) {
//                                        questionAnalysis.setIsCorrect(null);
//                                        stringBuilder = stringBuilder.append("未答题 ");
//                                        smallDto.setCorrectFlag("未答题");
//                                    } else {
//                                        questionAnalysis.setIsCorrect(judgeRes);
//                                        if (judgeRes) {
//                                            stringBuilder = stringBuilder.append("正确 ");
//                                            smallDto.setCorrectFlag("正确");
//                                        } else {
//                                            stringBuilder = stringBuilder.append("错误 ");
//                                            smallDto.setCorrectFlag("错误");
//                                        }
//                                    }
//
//                                } else {
//                                    String pamt = "作为一个作业批阅助手，请批阅该题：" + questionAnalysis.getContent() + ",参考答案：" + questionAnalysis.getReferenceAnswer()
//                                            + ",学生作答：" + questionAnalysis.getStudentAnswer() + ", 返回批阅结果，严格就判断正确与否";
//                                    String piyue = util.analyzeText(pamt);
//                                    if (piyue.contains("正确")) {
//                                        questionAnalysis.setIsCorrect(true);
//                                        stringBuilder = stringBuilder.append("正确 ");
//                                        smallDto.setCorrectFlag("正确");
//                                    } else {
//                                        questionAnalysis.setIsCorrect(false);
//                                        stringBuilder = stringBuilder.append("错误 ");
//                                        smallDto.setCorrectFlag("错误");
//                                    }
//                                }
//
//                            }
//                            String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();
//                            if (aiResultMap.containsKey(key)) {
//                                List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
//                                smallDtoList.add(smallDto);
//                                aiResultMap.put(key, smallDtoList);
//                            } else {
//                                List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
//                                smallDtoList.add(smallDto);
//                                aiResultMap.put(key, smallDtoList);
//                            }
//                            questionAnalysisService.save(questionAnalysis);
//                        }
//                        auditImages = stringBuilder.toString();
//                    } else {
//
//                        List<SubQuestionsEnt> answers = topicReport.getAnswers();
//                        for (SubQuestionsEnt temp : answers) {
//
//                            // 若库里没有试题和答案，则直接用AI阅卷，返回的内容出结果
//                            QuestionAnalysis questionAnalysis = new QuestionAnalysis();
//                            questionAnalysis.setId(null);
//                            if (StringUtils.isNotEmpty(questionAnalysis.getBigNumber()) && !map.containsKey(questionAnalysis.getBigNumber())) {
//                                stringBuilder = stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
//                                map.put(questionAnalysis.getBigNumber(), questionAnalysis.getQuestionType());
//                            }
//                            if (StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())) {
//                                stringBuilder = stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
//                            }
//                            questionAnalysis.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
//                            questionAnalysis.setStudentsHomeworkId(studentsHomework.getId());
//                            questionAnalysis.setClassesId(studentsHomework.getClassesId());
//                            questionAnalysis.setStudentId(studentsHomework.getStudentId());
//                            questionAnalysis.setStudentName(studentsHomework.getStudentName());
//                            questionAnalysis.setGrade(studentsHomework.getGrade());
//                            questionAnalysis.setSchoolId(studentsHomework.getSchoolId());
//                            questionAnalysis.setContent(temp.getQuestion_content());
//                            questionAnalysis.setBigNumber(temp.getMajor_question_id());
//                            questionAnalysis.setSmallNumber(temp.getQuestion_id());
//
//                            List<String> answerText = temp.getAnswer_text();
//                            questionAnalysis.setStudentAnswer(answerText != null ? String.join(",,,", answerText) : "");
//                            questionAnalysis.setReferenceAnswer(temp.getCorrect_answer() != null ? String.join(",,,", temp.getCorrect_answer()) : "");
//
//                            HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
//                            smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
//                            Boolean judgeRes = Boolean.valueOf(temp.getIs_correct());
//                            if (judgeRes == null) {
//                                questionAnalysis.setIsCorrect(null);
//                                smallDto.setCorrectFlag("未答题");
//                            } else {
//                                questionAnalysis.setIsCorrect(judgeRes);
//                                if (judgeRes) {
//                                    stringBuilder = stringBuilder.append("正确 ");
//                                    smallDto.setCorrectFlag("正确");
//                                } else {
//                                    stringBuilder = stringBuilder.append("错误 ");
//                                    smallDto.setCorrectFlag("错误");
//                                }
//                            }
//                            String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();
//                            if (aiResultMap.containsKey(key)) {
//                                List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
//                                smallDtoList.add(smallDto);
//                                aiResultMap.put(key, smallDtoList);
//                            } else {
//                                List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
//                                smallDtoList.add(smallDto);
//                                aiResultMap.put(key, smallDtoList);
//                            }
//                            questionAnalysisService.save(questionAnalysis);
//                        }
//                    }
//                } else {
//
//                    for (QuestionAnalysis questionAnalysis : questionAnalysisList) {
//                        SubQuestionsEnt stuWriteAnswer = getStuWriteAnswerNoStruc(topicReport.getAnswers(), questionAnalysis.getBigNumber(), questionAnalysis.getSmallNumber(), null);
//                        if (stuWriteAnswer != null) {
//                            List<String> stuAns = stuWriteAnswer.getAnswer_text();
//                            questionAnalysis.setStudentAnswer(CollectionUtils.isEmpty(stuAns) ? "未作答" : String.join(",,,", stuAns));
//                        }
//
//                        HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
//                        smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
//                        if (questionAnalysis.getStudentAnswer() == null ||
//                                questionAnalysis.getStudentAnswer().contains("未作答") ||
//                                questionAnalysis.getStudentAnswer().contains("未答题")) {
//                            questionAnalysis.setIsCorrect(null);
//                            stringBuilder = stringBuilder.append("未答题 ");
//                            smallDto.setCorrectFlag("未答题");
//                        } else {
//
//                            if ("选择题".equals(questionAnalysis.getQuestionType())
//                                    || "单选题".equals(questionAnalysis.getQuestionType()) || "多选题".equals(questionAnalysis.getQuestionType())
//                                    || "判断题".equals(questionAnalysis.getQuestionType())) {
//
//                                Boolean judgeRes = Boolean.valueOf(stuWriteAnswer.getIs_correct());
//                                if (judgeRes == null) {
//                                    questionAnalysis.setIsCorrect(null);
//                                    stringBuilder = stringBuilder.append("未答题 ");
//                                    smallDto.setCorrectFlag("未答题");
//                                } else {
//                                    questionAnalysis.setIsCorrect(judgeRes);
//                                    if (judgeRes) {
//                                        stringBuilder = stringBuilder.append("正确 ");
//                                        smallDto.setCorrectFlag("正确");
//                                    } else {
//                                        stringBuilder = stringBuilder.append("错误 ");
//                                        smallDto.setCorrectFlag("错误");
//                                    }
//                                }
//                            } else if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer()) && !"未作答".equals(questionAnalysis.getStudentAnswer())) {
////                            log.debug("作为一个作业批阅助手，请批阅该题!!!");
//                                String pamt = "作为一个作业批阅助手，请批阅该题：" + questionAnalysis.getContent() + ",参考答案：" + questionAnalysis.getReferenceAnswer()
//                                        + ",学生作答：" + questionAnalysis.getStudentAnswer() + ", 返回批阅结果，严格就判断正确与否";
//                                String piyue = util.analyzeText(pamt);
//                                if (piyue.contains("正确")) {
//                                    questionAnalysis.setIsCorrect(true);
//                                    stringBuilder = stringBuilder.append("正确 ");
//                                    smallDto.setCorrectFlag("正确");
//                                } else {
//                                    questionAnalysis.setIsCorrect(false);
//                                    stringBuilder = stringBuilder.append("错误 ");
//                                    smallDto.setCorrectFlag("错误");
//                                }
//                            } else {
//                                stringBuilder = stringBuilder.append("未答题 ");
//                                smallDto.setCorrectFlag("未答题");
//                            }
//
//                        }
//                        String key = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getQuestionType();
//
//
//                        if (aiResultMap.containsKey(key)) {
//                            List<HomeworkAISmallDto> smallDtoList = aiResultMap.get(key);
//                            smallDtoList.add(smallDto);
//                            aiResultMap.put(key, smallDtoList);
//                        } else {
//                            List<HomeworkAISmallDto> smallDtoList = new ArrayList<>();
//                            smallDtoList.add(smallDto);
//                            aiResultMap.put(key, smallDtoList);
//                        }
//                        questionAnalysisService.save(questionAnalysis);
//                    }
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }





        List<HomeworkAIBigDto> bigDtoList = new ArrayList<>();
        for(String key:aiResultMap.keySet()){
            String bigNumber = key.split(":")[0];
            String questionType = key.split(":")[1];
            HomeworkAIBigDto bigDto = new HomeworkAIBigDto();
            bigDto.setBigNumber(bigNumber);
            bigDto.setQuestionType(questionType);
            bigDto.setSmallDtoList(aiResultMap.get(key));
            bigDtoList.add(bigDto);
        }
        studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoList));
        studentsHomeworkNewRepository.save(studentsHomework);
        if(errorList!=null&&errorList.size()>0){
            for(QuestionAnalysis questionAnalysis:errorList){
                WrongTitleBook wrongTitleBook = new WrongTitleBook();
                wrongTitleBook.setTitleBigNo(questionAnalysis.getBigNumber());
                wrongTitleBook.setTitleSmallNo(questionAnalysis.getSmallNumber());
                wrongTitleBook.setTitleContext(questionAnalysis.getContent());
                wrongTitleBook.setStudentAnswer(questionAnalysis.getStudentAnswer());
                wrongTitleBook.setParse(questionAnalysis.getAnalysis());
                wrongTitleBook.setKnowledgePoint(questionAnalysis.getKnowledgePoints());
                wrongTitleBook.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                wrongTitleBook.setStudentsHomeworkId(studentsHomeworkId);
                wrongTitleBook.setSource("学生作业：" + studentsHomework.getHomeworkPublishName());
                wrongTitleBook.setStudentId(studentsHomework.getStudentId());
                wrongTitleBook.setStudentName(studentsHomework.getStudentName());
                wrongTitleBook.setClassId(studentsHomework.getClassesId());
                wrongTitleBook.setClassName(studentsHomework.getClassesName());
                wrongTitleBookService.addWrongBook(wrongTitleBook);
            }
        }
        return auditImages;
    }


    /*从AI返回内容，筛选匹配内容*/
    private SubQuestionsEnt getStuWriteAnswerNoStruc(List<SubQuestionsEnt> answers, String BigTopicId, String smallTopicId, String topicType){

        for (SubQuestionsEnt answer : answers) {
            String majorQuestionId = answer.getMajor_question_id();
            String questionId = answer.getQuestion_id();
            String question_type = answer.getQuestion_type();

            if(majorQuestionId.equalsIgnoreCase(BigTopicId) || questionId.equalsIgnoreCase(smallTopicId)){
                return answer;
            }
        }
        return null;
    }

    /*从AI返回内容，筛选匹配内容*/
    private ZhiPuAIAgent.SubQuestions getStuWriteAnswer(List<ZhiPuAIAgent.SubQuestions> answers,String BigTopicId,String smallTopicId,String topicType){

        for (ZhiPuAIAgent.SubQuestions answer : answers) {
            String majorQuestionId = answer.major_question_id();
            String questionId = answer.question_id();
            String question_type = answer.question_type();

            if(majorQuestionId.equalsIgnoreCase(BigTopicId) || questionId.equalsIgnoreCase(smallTopicId)){
                return answer;
            }
        }
        return null;
    }

    @Override
    public Long getSubmitNumByHomeworkPublishId(Long homeworkPublishId) {
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                Predicate condition1 = criteriaBuilder.isNotNull(root.get("submitTime"));
                query.where(condition0,condition1);
                return null;
            }


        };
        return studentsHomeworkNewRepository.count(specification);
    }

    @Override
    public void saveErrorTitleRecords(Long studentId, String subject, List<StudentsWriteRecord> uploadErrorTitleRecords) {
        WrongTitleWriteData wrongTitleWriteData = new WrongTitleWriteData();
        wrongTitleWriteData.setStudentId(studentId);
        wrongTitleWriteData.setSubject(subject);
        wrongTitleWriteData.setStudentsWriteRecords(uploadErrorTitleRecords);
        wrongTitleWriteData.setCreateTime(new Date());
        wrongTitleWriteDataService.save(wrongTitleWriteData);

        FutureTask<String> futureTask = new FutureTask<>(() -> {
            String auditImages = "";
            //异步处理AI智能审批
            //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
            //QianWenAIUtil util = new QianWenAIUtil();
            AIUtil util  = aiUtil.getAIUtil();
            if("qianwen".equals(util.getAiName())){
                util = (QianWenAIUtil)  util;
            }else {
                util = (ZhipuAIImageAnalysisUtil) util;
            }
            ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
            BufferedImage image = WritingDataRenderer.drawWritingData(uploadErrorTitleRecords, 794, 1123);
            String imageUrl = "错题上传-"+studentId+".png";
            CoordinateImageGenerator.saveImage(image,imageUrl);
            try {
                String prompt = "解析图片笔记内容， 按每道题分割返回，每道题返回内容格式为 " +
                        "错题分析：大题号： 小题号：  题内容： 学生答案：  解析：     \n";
                String scoreAndAccuracy = util.analyzeImage(imageUrl,prompt);
                String wrongTitleStr = scoreAndAccuracy.substring(scoreAndAccuracy.indexOf("错题分析："));
                System.out.println("错题分析: " + wrongTitleStr);
                String[] wrongTitleList = wrongTitleStr.split("\n");
                for(int i=0;i<wrongTitleList.length;i++) {
                    String wrongTitle = wrongTitleList[i];
                    if (StringUtils.isNotEmpty(wrongTitle.trim())&&wrongTitle.contains("大题号：")&&wrongTitle.contains("解析：")&&wrongTitle.contains("解析：")) {
                        String titleBigNo = wrongTitle.substring(wrongTitle.indexOf("大题号：") + 4, wrongTitle.indexOf("小题号："));
                        String titleSmallNo = wrongTitle.substring(wrongTitle.indexOf("小题号：") + 4, wrongTitle.indexOf("题内容："));
                        String titleContext = wrongTitle.substring(wrongTitle.indexOf("题内容：") + 4, wrongTitle.indexOf("学生答案：")).trim();
                        String studentAnswer = wrongTitle.substring(wrongTitle.indexOf("学生答案：") + 5, wrongTitle.indexOf("解析：")).trim();
                        String parse = wrongTitle.substring(wrongTitle.indexOf("解析：") + 3).trim();
                        WrongTitleBook wrongTitleBook = new WrongTitleBook();
                        wrongTitleBook.setTitleBigNo(titleBigNo);
                        wrongTitleBook.setTitleSmallNo(titleSmallNo);
                        wrongTitleBook.setTitleContext(titleContext);
                        wrongTitleBook.setStudentAnswer(studentAnswer);
                        wrongTitleBook.setParse(parse);
                        wrongTitleBook.setSource("学生智能手写板上传");
                        wrongTitleBook.setWriteDataId(wrongTitleWriteData.getId());
                        wrongTitleBook.setStudentId(studentId);
                        if(resultDto!=null&&resultDto.getData()!=null) {
                            Student student = resultDto.getData();
                            wrongTitleBook.setStudentName(student.getStudentName());
                            wrongTitleBook.setClassId(student.getClassesId());
                            wrongTitleBook.setClassName(student.getClassesName());
                        }
                        wrongTitleBookService.addWrongBook(wrongTitleBook);
                    }
                }
                File file = new File(imageUrl);
                file.delete();
            } catch (Exception e) {
                System.out.println("+++++解析分数错误++++++++++ "+e.getMessage() );
            }



            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
    }

    @Override
    public StudentsHomeworkNew appSubmit(StudentsHomeworkNew studentsHomework) {
        studentsHomework =studentsHomeworkNewRepository.save(studentsHomework);
        StudentsHomeworkNew finalStudentsHomework = studentsHomework;
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
            //QianWenAIUtil util  = new QianWenAIUtil();
            AIUtil util  = aiUtil.getAIUtil();
            if("qianwen".equals(util.getAiName())){
                util = (QianWenAIUtil)  util;
            }else {
                util = (ZhipuAIImageAnalysisUtil) util;
            }
            try{
                String auditImages ="";
                if(StringUtils.isNotEmpty(finalStudentsHomework.getSubmitFileUrl())) {
                    List<String> imageNames = Arrays.asList(finalStudentsHomework.getSubmitFileUrl().split(","));
                    List<QuestionAnalysis> analyses=util.batchReviewExamQuestions(imageNames);

                    List<QuestionAnalysis> errorList = new ArrayList<>();
                    if(analyses!=null&&analyses.size()>0){

                        System.out.println(analyses.toString());
                        Map<String,String> map = new HashMap<>();
                        StringBuilder stringBuilder = new StringBuilder();
                        for(QuestionAnalysis questionAnalysis:analyses){
                            if(StringUtils.isNotEmpty(questionAnalysis.getBigNumber())&&!map.containsKey(questionAnalysis.getBigNumber())){
                                stringBuilder = stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
                                map.put(questionAnalysis.getBigNumber(),questionAnalysis.getQuestionType());
                            }
                            if(StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())){
                                stringBuilder = stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
                            }
                            questionAnalysis.setHomeworkPublishId(finalStudentsHomework.getHomeworkPublishId());
                            questionAnalysis.setStudentsHomeworkId(finalStudentsHomework.getId());
                            questionAnalysis.setClassesId(finalStudentsHomework.getClassesId());
                            questionAnalysis.setStudentId(finalStudentsHomework.getStudentId());
                            questionAnalysis.setStudentName(finalStudentsHomework.getStudentName());
                            questionAnalysis.setGrade(finalStudentsHomework.getGrade());
                            questionAnalysis.setSchoolId(finalStudentsHomework.getSchoolId());
                            if(questionAnalysis.getIsCorrect()==null){
                                stringBuilder = stringBuilder.append("未答题 ");
                            }else if(questionAnalysis.getIsCorrect()){
                                stringBuilder = stringBuilder.append("正确 ");
                            }else{
                                stringBuilder = stringBuilder.append("错误 ");
                                errorList.add(questionAnalysis);
                            }
                            questionAnalysisService.save(questionAnalysis);
                        }
                        auditImages = stringBuilder.toString();
                    }
                    finalStudentsHomework.setAiAudit(auditImages);
                    studentsHomeworkNewRepository.save(finalStudentsHomework);
                    if(errorList!=null&&errorList.size()>0){
                        for(QuestionAnalysis questionAnalysis:errorList){
                            WrongTitleBook wrongTitleBook = new WrongTitleBook();
                            wrongTitleBook.setTitleBigNo(questionAnalysis.getBigNumber());
                            wrongTitleBook.setTitleSmallNo(questionAnalysis.getSmallNumber());
                            wrongTitleBook.setTitleContext(questionAnalysis.getContent());
                            wrongTitleBook.setStudentAnswer(questionAnalysis.getStudentAnswer());
                            wrongTitleBook.setParse(questionAnalysis.getAnalysis());
                            wrongTitleBook.setKnowledgePoint(questionAnalysis.getKnowledgePoints());
                            wrongTitleBook.setHomeworkPublishId(finalStudentsHomework.getHomeworkPublishId());
                            wrongTitleBook.setStudentsHomeworkId(finalStudentsHomework.getId());
                            wrongTitleBook.setSource("学生作业：" + finalStudentsHomework.getHomeworkPublishName());
                            wrongTitleBook.setStudentId(finalStudentsHomework.getStudentId());
                            wrongTitleBook.setStudentName(finalStudentsHomework.getStudentName());
                            wrongTitleBook.setClassId(finalStudentsHomework.getClassesId());
                            wrongTitleBook.setClassName(finalStudentsHomework.getClassesName());
                            wrongTitleBookService.addWrongBook(wrongTitleBook);
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return studentsHomework;
    }

    @Override
    public StudentsHomeworkNew appEmendSubmit(StudentsHomeworkNew studentsHomework) {
        studentsHomework =studentsHomeworkNewRepository.save(studentsHomework);
        StudentsHomeworkNew finalStudentsHomework = studentsHomework;
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
            //QianWenAIUtil util = new QianWenAIUtil();
            AIUtil util  = aiUtil.getAIUtil();
            if("qianwen".equals(util.getAiName())){
                util = (QianWenAIUtil)  util;
            }else {
                util = (ZhipuAIImageAnalysisUtil) util;
            }
            try{
                String auditImages ="";
                if(StringUtils.isNotEmpty(finalStudentsHomework.getSubmitFileUrl2())) {
                    List<String> imageNames = Arrays.asList(finalStudentsHomework.getSubmitFileUrl2().split(","));
                    List<HomeworkAIBigDto> bigDtoAll  = new ArrayList<>();
                    Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                    for (String key : resltMap.keySet()) {
                        String titleImage = resltMap.get(key);
                        if(titleImage.contains("<|begin_of_box|>")){
                            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>")+16);
                        }
                        if (titleImage.contains("<|end_of_box|>")) {
                            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                        }
                        List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                        bigDtoAll.addAll(bigDtoList);
                        auditImages = auditImages + titleImage;
                        File imageFile = new File(key);
                        imageFile.delete();
                    }
                    System.out.println("批阅结果: " + auditImages);

                    finalStudentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
                    studentsHomeworkNewRepository.save(finalStudentsHomework);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return studentsHomework;
    }

    @Override
    public String aIauditEmend(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = this.getById(studentsHomeworkId);
        String auditImages = "";
        //异步处理AI智能审批
        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        //QianWenAIUtil util = new QianWenAIUtil();
        AIUtil util  = aiUtil.getAIUtil();
        if("qianwen".equals(util.getAiName())){
            util = (QianWenAIUtil)  util;
        }else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"2");
        try {
            if(StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl2())) {
                List<String> imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl2().split(","));
                List<HomeworkAIBigDto> bigDtoAll  = new ArrayList<>();
                Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                for (String key : resltMap.keySet()) {
                    String titleImage = resltMap.get(key);
                    if(titleImage.contains("<|begin_of_box|>")){
                        titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>")+16);
                    }
                    if (titleImage.contains("<|end_of_box|>")) {
                        titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                    }
                    List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                    bigDtoAll.addAll(bigDtoList);
                    auditImages = auditImages + titleImage;
                    File imageFile = new File(key);
                    imageFile.delete();
                }
                System.out.println("批阅结果: " + auditImages);
            }else if(!homeworkStudentWriteDataList.isEmpty()){
                List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
                if (studentsHomework.getTopicImages() != null && studentsHomework.getTopicImages().size() > 0
                        && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
                    try {
                        List<String> imageNames = new ArrayList<>();
                        for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
                            String imageUrl = studentsHomework.getTopicImages().get(i);


                            for (HomeworkStudentWriteData writeData1 : homeworkStudentWriteDataList) {
                                if (writeData1.getPageNum() == (i + 1)) {
                                    List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                                    BufferedImage resultImage = null;

                                    resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);

                                    // 保存结果图片
                                    String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                                    CoordinateImageGenerator.saveImage(resultImage, imageName);
                                    imageNames.add(imageName);
                                }
                            }
                        }
                        Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                        //Map<String, String> resltMap = util.analyzeImageToJson(imageNames,"");
                        for (String key : resltMap.keySet()) {
                            String titleImage = resltMap.get(key);
                            if (titleImage.contains("<|begin_of_box|>")) {
                                titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                            }
                            if (titleImage.contains("<|end_of_box|>")) {
                                titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                            }
                            List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                            auditImages = auditImages + titleImage;
                            File imageFile = new File(key);
                            imageFile.delete();
                        }

                        System.out.println("批阅结果: " + auditImages);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                    try {
                        String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                        DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                        //试题识别
                        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
                        String titleImage = util.analyzeImage(outputPath,prompt);
                        if (titleImage.contains("<|begin_of_box|>")) {
                            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                        }
                        if (titleImage.contains("<|end_of_box|>")) {
                            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                        }
                        System.out.println("批阅结果: " + titleImage);
                        if(titleImage.startsWith("[")) {
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                        }else if(titleImage.startsWith("{")){
                            HomeworkAIBigDto bigDto= JSONObject.parseObject(titleImage,HomeworkAIBigDto.class);
                            bigDtoAll.add(bigDto);
                        }else{
                            System.out.println("json格式不对："+titleImage);
                        }
                        auditImages = auditImages + "\n" + titleImage;
                        File imageFile = new File(outputPath);
                        imageFile.delete();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
            }
            studentsHomeworkNewRepository.save(studentsHomework);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return auditImages;
    }

    @Override
    public String aIauditEmendStruc(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = this.getById(studentsHomeworkId);
        String auditImages = "";
        //异步处理AI智能审批
        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        //QianWenAIUtil util = new QianWenAIUtil();
        AIUtil util  = aiUtil.getAIUtil();
        if("qianwen".equals(util.getAiName())){
            util = (QianWenAIUtil)  util;
        }else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList=homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(),"2");
        try {
            if(StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl2())) {
                List<String> imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl2().split(","));
                List<HomeworkAIBigDto> bigDtoAll  = new ArrayList<>();

                Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);

                for (String key : resltMap.keySet()) {
                    String titleImage = resltMap.get(key);
                    if(titleImage.contains("<|begin_of_box|>")){
                        titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>")+16);
                    }
                    if (titleImage.contains("<|end_of_box|>")) {
                        titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                    }
                    List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                    bigDtoAll.addAll(bigDtoList);
                    auditImages = auditImages + titleImage;
                    File imageFile = new File(key);
                    imageFile.delete();
                }
                System.out.println("批阅结果: " + auditImages);
            }else if(!homeworkStudentWriteDataList.isEmpty()){
                List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
                if (studentsHomework.getTopicImages() != null && studentsHomework.getTopicImages().size() > 0
                        && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
                    try {
                        List<String> imageNames = new ArrayList<>();
                        for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
                            String imageUrl = studentsHomework.getTopicImages().get(i);


                            for (HomeworkStudentWriteData writeData1 : homeworkStudentWriteDataList) {
                                if (writeData1.getPageNum() == (i + 1)) {
                                    List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                                    BufferedImage resultImage = null;

                                    resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);

                                    // 保存结果图片
                                    String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                                    CoordinateImageGenerator.saveImage(resultImage, imageName);
                                    imageNames.add(imageName);
                                }
                            }
                        }
                        Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                        //Map<String, String> resltMap = util.analyzeImageToJson(imageNames,"");
                        for (String key : resltMap.keySet()) {
                            String titleImage = resltMap.get(key);
                            if (titleImage.contains("<|begin_of_box|>")) {
                                titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                            }
                            if (titleImage.contains("<|end_of_box|>")) {
                                titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                            }
                            List<HomeworkAIBigDto> bigDtoList= JSONArray.parseArray(titleImage,HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                            auditImages = auditImages + titleImage;
                            File imageFile = new File(key);
                            imageFile.delete();
                        }

                        System.out.println("批阅结果: " + auditImages);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                    try {
                        String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                        DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                        //试题识别
                        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
                        String titleImage = util.analyzeImage(outputPath,prompt);
                        if (titleImage.contains("<|begin_of_box|>")) {
                            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                        }
                        if (titleImage.contains("<|end_of_box|>")) {
                            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                        }
                        System.out.println("批阅结果: " + titleImage);
                        if(titleImage.startsWith("[")) {
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                        }else if(titleImage.startsWith("{")){
                            HomeworkAIBigDto bigDto= JSONObject.parseObject(titleImage,HomeworkAIBigDto.class);
                            bigDtoAll.add(bigDto);
                        }else{
                            System.out.println("json格式不对："+titleImage);
                        }
                        auditImages = auditImages + "\n" + titleImage;
                        File imageFile = new File(outputPath);
                        imageFile.delete();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
            }
            studentsHomeworkNewRepository.save(studentsHomework);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return auditImages;
    }


    @Override
    public Page<StudentsHomeworkSimpleDTO> getEmendPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentsHomeworkSimpleDTO> criteriaQuery = criteriaBuilder.createQuery(StudentsHomeworkSimpleDTO.class);
        Root<StudentsHomeworkNew> root = criteriaQuery.from(StudentsHomeworkNew.class);

        // 使用投影查询，只选择需要的字段
        criteriaQuery.multiselect(
            root.get("id"),
            root.get("homeworkType"),
            root.get("homeworkPublishId"),
            root.get("homeworkPublishName"),
            root.get("combinationQuestionsId"),
            root.get("designId"),
            root.get("schoolId"),
            root.get("grade"),
            root.get("classesId"),
            root.get("classesName"),
            root.get("studentId"),
            root.get("studentName"),
            root.get("studentUuid"),
            root.get("submitFileUrl"),
            root.get("startTime"),
            root.get("submitStatus"),
            root.get("submitTime"),
            root.get("createTime"),
            root.get("auditStatus"),
            root.get("auditTime"),
            root.get("errorReason"),
            root.get("suggestion"),
            root.get("design_file_id"),
            root.get("teacherAuditSuggest"),
            root.get("teacherAuditLevel"),
            root.get("subject"),
            root.get("accuracy"),
            root.get("classRank"),
            root.get("deadline"),
            root.get("commentText"),
            root.get("chapter"),
            root.get("knowledgePoint"),
            root.get("emendStatus"),
            root.get("dailyPracticeld"),
            root.get("dailyPracticeName"),
            root.get("score")
        );

        // 构建查询条件，与getStudentsHomeworkPage保持一致
        List<Predicate> predicates = new ArrayList<>();

        // 学校ID条件
        Predicate schoolCondition = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
        predicates.add(schoolCondition);
        if(studentsHomework != null && StringUtils.isNotEmpty(studentsHomework.getHomeworkPublishName())){
            Predicate nameCondition = criteriaBuilder.like(root.get("homeworkPublishName"), "%"+studentsHomework.getHomeworkPublishName()+"%");
            predicates.add(nameCondition);
        }
        // 审批状态条件
        if (studentsHomework != null && studentsHomework.getAuditStatus() != null) {
            Predicate auditStatusCondition = criteriaBuilder.equal(root.get("auditStatus"), studentsHomework.getAuditStatus());
            predicates.add(auditStatusCondition);

            // 如果审批状态>=2，要求auditTime不为空
            if (studentsHomework.getAuditStatus() >= 2) {
                Predicate auditTimeCondition = criteriaBuilder.isNotNull(root.get("auditTime"));
                predicates.add(auditTimeCondition);
            }
        }

        // 班级ID条件
        if (studentsHomework != null && studentsHomework.getClassesId() != null) {
            Predicate classesCondition = criteriaBuilder.equal(root.get("classesId"), studentsHomework.getClassesId());
            predicates.add(classesCondition);
        }

        // 学生ID条件
        if (studentsHomework != null && studentsHomework.getStudentId() != null) {
            Predicate studentCondition = criteriaBuilder.equal(root.get("studentId"), studentsHomework.getStudentId());
            predicates.add(studentCondition);
        }

        // 创建时间条件
        if (studentsHomework != null && studentsHomework.getCreateTime() != null) {
            Date createTime = studentsHomework.getCreateTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(createTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date createTime1 = calendar.getTime();
            Predicate timeCondition = criteriaBuilder.between(root.get("createTime").as(Date.class), createTime, createTime1);
            predicates.add(timeCondition);
        }

        // 订正状态条件
        if (studentsHomework != null && studentsHomework.getEmendStatus() != null) {
            Predicate emendStatusCondition = criteriaBuilder.isNotEmpty(root.get("emendStatus"));
            predicates.add(emendStatusCondition);
        }

        // 知识点条件
        if (studentsHomework != null && StringUtils.isNotEmpty(studentsHomework.getKnowledgePoint())) {
            Predicate knowledgePointCondition = criteriaBuilder.like(root.get("knowledgePoint"), "%" + studentsHomework.getKnowledgePoint() + "%");
            predicates.add(knowledgePointCondition);
        }
        Predicate statusNotNUll=criteriaBuilder.isNotNull(root.get("submitStatus"));
        predicates.add(statusNotNUll);
        // 应用所有条件
        criteriaQuery.where(predicates.toArray(new Predicate[0]));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("createTime")));

        // 执行查询并获取分页结果
        TypedQuery<StudentsHomeworkSimpleDTO> typedQuery = entityManager.createQuery(criteriaQuery);

        // 计算总数 - 创建独立的谓词列表以避免路径冲突
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<StudentsHomeworkNew> countRoot = countQuery.from(StudentsHomeworkNew.class);

        // 为计数查询创建新的谓词列表
        List<Predicate> countPredicates = new ArrayList<>();

        // 学校ID条件
        if (userService.getCurrentSchoolIdSafely() != null) {
            Predicate schoolCountCondition = criteriaBuilder.equal(countRoot.get("schoolId"), userService.getCurrentSchoolIdSafely());
            countPredicates.add(schoolCountCondition);
        }

        // 审批状态条件
        if (studentsHomework != null && studentsHomework.getAuditStatus() != null) {
            Predicate auditStatusCountCondition = criteriaBuilder.equal(countRoot.get("auditStatus"), studentsHomework.getAuditStatus());
            countPredicates.add(auditStatusCountCondition);

            // 如果审批状态>=2，要求auditTime不为空
            if (studentsHomework.getAuditStatus() >= 2) {
                Predicate auditTimeCountCondition = criteriaBuilder.isNotNull(countRoot.get("auditTime"));
                countPredicates.add(auditTimeCountCondition);
            }
        }

        // 班级ID条件
        if (studentsHomework != null && studentsHomework.getClassesId() != null) {
            Predicate classesCountCondition = criteriaBuilder.equal(countRoot.get("classesId"), studentsHomework.getClassesId());
            countPredicates.add(classesCountCondition);
        }

        // 学生ID条件
        if (studentsHomework != null && studentsHomework.getStudentId() != null) {
            Predicate studentCountCondition = criteriaBuilder.equal(countRoot.get("studentId"), studentsHomework.getStudentId());
            countPredicates.add(studentCountCondition);
        }

        // 创建时间条件
        if (studentsHomework != null && studentsHomework.getCreateTime() != null) {
            Date createTime = studentsHomework.getCreateTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(createTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date createTime1 = calendar.getTime();
            Predicate timeCountCondition = criteriaBuilder.between(countRoot.get("createTime").as(Date.class), createTime, createTime1);
            countPredicates.add(timeCountCondition);
        }

        // 订正状态条件
        if (studentsHomework != null && studentsHomework.getEmendStatus() != null) {
            Predicate emendStatusCountCondition = criteriaBuilder.isNotEmpty(countRoot.get("emendStatus"));
            countPredicates.add(emendStatusCountCondition);
        }

        // 知识点条件
        if (studentsHomework != null && StringUtils.isNotEmpty(studentsHomework.getKnowledgePoint())) {
            Predicate knowledgePointCountCondition = criteriaBuilder.like(countRoot.get("knowledgePoint"), "%" + studentsHomework.getKnowledgePoint() + "%");
            countPredicates.add(knowledgePointCountCondition);
        }
        Predicate statusNotNUll2=criteriaBuilder.isNotNull(countRoot.get("submitStatus"));
        countPredicates.add(statusNotNUll2);
        countQuery.select(criteriaBuilder.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // 设置分页参数
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // 获取当前页数据
        List<StudentsHomeworkSimpleDTO> content = typedQuery.getResultList();

        // 返回分页结果
        return new PageImpl<>(content, pageable, totalCount);
    }
}
