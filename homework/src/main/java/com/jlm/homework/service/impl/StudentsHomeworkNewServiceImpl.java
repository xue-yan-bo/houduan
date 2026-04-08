package com.jlm.homework.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.*;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.hibernate.query.Order;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StudentsHomeworkNewServiceImpl implements IStudentsHomeworkNewService {
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;

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
    private IStudentHomeworkAIService studentHomeworkAIService;
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

                    // 批量保存学生作业记录
                    List<StudentsHomeworkNew> studentsHomeworkList = new ArrayList<>();
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
                        studentsHomeworkList.add(studentsHomework);
                    }

                    // 批量保存
                    if(!studentsHomeworkList.isEmpty()){
                        studentsHomeworkNewRepository.saveAll(studentsHomeworkList);
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
        Specification<StudentsHomeworkNew> specification = (root, query, criteriaBuilder) ->
                PredicateBuilderUtil.buildStudentHomeworkPredicate(
                        criteriaBuilder, root,
                        homeworkPublishId,
                        studentsHomeworkRequest != null ? studentsHomeworkRequest.getStudentName() : null,
                        studentsHomeworkRequest != null ? studentsHomeworkRequest.getSubmitStatus() : null,
                        studentsHomeworkRequest != null ? studentsHomeworkRequest.getSubmitTime() : null,
                        studentsHomeworkRequest != null ? studentsHomeworkRequest.getAuditTime() : null,
                        studentsHomeworkRequest != null ? studentsHomeworkRequest.getAuditStatus() : null
                );
        return this.findAllSimpleDTOBySpecification(specification);
    }

    @Override
    public StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework) {
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);

        // 清除缓存
        String cacheKey = "studentsHomework:id:" + studentsHomework.getId();
        redisTemplate.delete(cacheKey);

        HomeworkPublish homeworkPublish = homeworkPublishRepository.getById(studentsHomework.getHomeworkPublishId());
        StudentsHomeworkNew finalStudentsHomework = studentsHomework;
        Specification<StudentsHomeworkNew> specification = new Specification<StudentsHomeworkNew>() {
            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition = criteriaBuilder.equal(root.get("homeworkPublishId"), finalStudentsHomework.getHomeworkPublishId());
                list.add(condition);
                Predicate condition1 = criteriaBuilder.le(root.get("auditStatus"), 1);
                list.add(condition1);
                Predicate[] p = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        long count = studentsHomeworkNewRepository.count(specification);
        if (count == 0) {
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
        List<StudentsHomeworkNew> content = studentsHomeworkList.getContent();
        if(!content.isEmpty()){
            // 提取所有学生作业ID
            List<Long> studentsHomeworkIds = content.stream()
                    .map(StudentsHomeworkNew::getId)
                    .collect(Collectors.toList());

            // 批量查询 HomeworkStudentWriteData（类型1）
            Map<Long, List<HomeworkStudentWriteData>> writeDataMap1 = homeworkStudentWriteDataService.findByStudentRecordIds(studentsHomeworkIds, "1");

            // 批量查询 StudentsHomeworkCorrect（类型1）
            List<StudentsHomeworkCorrect> allCorrects = studentsHomeworkCorrectRepository.findByStudentsHomeworkIdInAndType(studentsHomeworkIds, 1);
            Map<Long, List<StudentsHomeworkCorrect>> correctMap = new HashMap<>();
            for (StudentsHomeworkCorrect correct : allCorrects) {
                correctMap.computeIfAbsent(correct.getStudentsHomeworkId(), k -> new ArrayList<>())
                         .add(correct);
            }

            // 设置数据
            for(StudentsHomeworkNew studentsHomework: content){
                // 设置学生写作数据
                List<HomeworkStudentWriteData> writeDatas = writeDataMap1.getOrDefault(studentsHomework.getId(), new ArrayList<>());
                studentsHomework.setStudentWriteDataList(writeDatas);

                // 设置批改数据
                List<StudentsHomeworkCorrect> correctList = correctMap.getOrDefault(studentsHomework.getId(), new ArrayList<>());
                studentsHomework.setHomeworkCorrectList(correctList);
            }
        }
        return studentsHomeworkList;
    }

    @Override
    public Page<StudentsHomeworkNew> getClassHomeworkStatistics(Integer pageNum, Integer pageSize, String subject, Long classId, String startDate, String endDate) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 先查询符合条件的 homeworkPublishIds
        List<Long> homeworkPublishIds = new ArrayList<>();
        if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
            Specification<HomeworkPublish> publishSpec = new Specification<HomeworkPublish>() {
                @Override
                public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    List<Predicate> predicates = new ArrayList<>();

                    if (StringUtils.isNotEmpty(subject)) {
                        predicates.add(criteriaBuilder.equal(root.get("subject"), subject));
                    }

                    if (classId != null) {
                        predicates.add(criteriaBuilder.like(root.get("classIds"), "%" + classId + "%"));
                    }

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date start = sdf.parse(startDate + " 00:00:00");
                        Date end = sdf.parse(endDate + " 23:59:59");
                        predicates.add(criteriaBuilder.between(root.get("publishTime"), start, end));
                    } catch (ParseException e) {
                        log.error("日期解析失败: startDate={}, endDate={}", startDate, endDate, e);
                    }

                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                }
            };

            List<HomeworkPublish> homeworkPublishList = homeworkPublishRepository.findAll(publishSpec);
            if (!homeworkPublishList.isEmpty()) {
                homeworkPublishIds = homeworkPublishList.stream()
                        .map(HomeworkPublish::getId)
                        .collect(Collectors.toList());
            } else {
                return Page.empty();
            }
        }

        // 构建学生作业查询条件
        List<Long> finalHomeworkPublishIds = homeworkPublishIds;
        Specification<StudentsHomeworkNew> studentsSpec = new Specification<StudentsHomeworkNew>() {
            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                // 作业发布ID条件
                if (!finalHomeworkPublishIds.isEmpty()) {
                    predicates.add(root.get("homeworkPublishId").in(finalHomeworkPublishIds));
                }
                // 班级条件
                if (classId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("classesId"), classId));
                }
                // 科目条件
                if (StringUtils.isNotEmpty(subject)) {
                    predicates.add(criteriaBuilder.equal(root.get("subject"), subject));
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };

        // 排序和分页
        Sort studentSort = Sort.by(Sort.Direction.DESC, "accuracy", "createTime");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, studentSort);
        
        // 数据库分页查询
        Page<StudentsHomeworkNew> studentsHomeworkPage = studentsHomeworkNewRepository.findAll(studentsSpec, pageable);
        
        return studentsHomeworkPage;
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
            if(!homeworkList.isEmpty()){
                // 提取所有学生作业ID
                List<Long> studentsHomeworkIds = homeworkList.stream()
                        .map(StudentsHomeworkNew::getId)
                        .collect(Collectors.toList());

                // 批量查询 HomeworkStudentWriteData（类型1和2）
                Map<Long, List<HomeworkStudentWriteData>> writeDataMap1 = homeworkStudentWriteDataService.findByStudentRecordIds(studentsHomeworkIds, "1");
                Map<Long, List<HomeworkStudentWriteData>> writeDataMap2 = homeworkStudentWriteDataService.findByStudentRecordIds(studentsHomeworkIds, "2");

                // 批量查询 StudentsHomeworkCorrect（类型1和2）
                List<StudentsHomeworkCorrect> allCorrects1 = studentsHomeworkCorrectRepository.findByStudentsHomeworkIdInAndType(studentsHomeworkIds, 1);
                List<StudentsHomeworkCorrect> allCorrects2 = studentsHomeworkCorrectRepository.findByStudentsHomeworkIdInAndType(studentsHomeworkIds, 2);

                Map<Long, List<StudentsHomeworkCorrect>> correctMap1 = new HashMap<>();
                for (StudentsHomeworkCorrect correct : allCorrects1) {
                    correctMap1.computeIfAbsent(correct.getStudentsHomeworkId(), k -> new ArrayList<>())
                             .add(correct);
                }

                Map<Long, List<StudentsHomeworkCorrect>> correctMap2 = new HashMap<>();
                for (StudentsHomeworkCorrect correct : allCorrects2) {
                    correctMap2.computeIfAbsent(correct.getStudentsHomeworkId(), k -> new ArrayList<>())
                             .add(correct);
                }

                // 设置数据
                for (StudentsHomeworkNew homework : homeworkList) {
                    // 设置学生写作数据
                    List<HomeworkStudentWriteData> writeDatas1 = writeDataMap1.getOrDefault(homework.getId(), new ArrayList<>());
                    homework.setStudentWriteDataList(writeDatas1);

                    List<HomeworkStudentWriteData> writeDatas2 = writeDataMap2.getOrDefault(homework.getId(), new ArrayList<>());
                    homework.setStudentWriteDataList2(writeDatas2);

                    // 设置批改数据
                    List<StudentsHomeworkCorrect> correctList1 = correctMap1.getOrDefault(homework.getId(), new ArrayList<>());
                    homework.setHomeworkCorrectList(correctList1);

                    List<StudentsHomeworkCorrect> correctList2 = correctMap2.getOrDefault(homework.getId(), new ArrayList<>());
                    homework.setHomeworkCorrectList2(correctList2);
                }
            }
        }
        return studentsHomeworkList;
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PDFUtil pdfUtil;

    @Override
    public StudentsHomeworkNew getById(Long id) {
        String cacheKey = "studentsHomework:id:" + id;

        // 尝试从缓存获取
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null && cachedObj instanceof StudentsHomeworkNew) {
            return (StudentsHomeworkNew) cachedObj;
        }

        // 从数据库查询
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(id).orElse(null);
        if (studentsHomework != null) {
            List<HomeworkStudentWriteData> writeDatas = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "1");
            studentsHomework.setStudentWriteDataList(writeDatas);
            List<HomeworkStudentWriteData> writeDatas2 = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "2");
            studentsHomework.setStudentWriteDataList2(writeDatas2);
            StudentsHomeworkCorrect search = new StudentsHomeworkCorrect();
            search.setStudentsHomeworkId(studentsHomework.getId());
            search.setType(1);
            List<StudentsHomeworkCorrect> correctList = studentsHomeworkCorrectRepository.findAll(Example.of(search));
            studentsHomework.setHomeworkCorrectList(correctList);
            search.setType(2);
            List<StudentsHomeworkCorrect> correctList2 = studentsHomeworkCorrectRepository.findAll(Example.of(search));
            studentsHomework.setHomeworkCorrectList2(correctList2);

            // 缓存结果，设置1小时过期
            redisTemplate.opsForValue().set(cacheKey, studentsHomework, 1, TimeUnit.HOURS);
        }
        return studentsHomework;
    }

    @Override
    public void endStudentsHomework(HomeworkPublish homeworkPublish) {
        studentsHomeworkNewRepository.updateDeadline(homeworkPublish.getId(),new Date(),2);
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
            homeWork2Board.setHomeworkId(homework.getId());
            homeWork2Board.setHomeworkName(homework.getHomeworkPublishName());
            homeWork2Board.setSubject(homework.getSubject());
            if(homework.getTopicImages()!=null&&homework.getTopicImages().size()>0){
                if(homework.getTopicImages()!=null&&homework.getTopicImages().size()>0){
                    int size = 0;
                    for(String url:homework.getTopicImages()){
                        if(url.endsWith(".pdf")){
                            //获取PDF页数
                            int pdfSize = pdfUtil.getPdfPageCount(url);
                            size = size + pdfSize;
                        }else{
                            size++;
                        }
                    }
                    homeWork2Board.setPageSize(size);
                }else{
                    homeWork2Board.setPageSize(1);
                }
            }else{
                homeWork2Board.setPageSize(1);
            }

            homeWork2Boards.add(homeWork2Board);
        }
        return homeWork2Boards;
    }

    @Override
    public void saveWriteRecords(Long studentId, Long homeworkId, String type, Integer pageN, List<StudentsWriteRecord> studentsWriteRecords, Boolean isFinish) {
        log.info("开始保存书写记录，studentId: {}, homeworkId: {}, type: {}", studentId, homeworkId, type);

        Optional<StudentsHomeworkNew> optional = studentsHomeworkNewRepository.findById(homeworkId);
        if (optional == null || optional.isEmpty()) {
            log.warn("作业不存在，homeworkId: {}", homeworkId);
            return;
        }

        StudentsHomeworkNew studentsHomework = optional.get();

        // 保存书写记录
        HomeworkStudentWriteData writeData = new HomeworkStudentWriteData();
        writeData.setStudentHomeworkId(studentsHomework.getId());
        writeData.setPageNum(pageN);
        writeData.setStudentName(studentsHomework.getStudentName());
        writeData.setStudentId(studentId);
        writeData.setStudentsWriteRecords(studentsWriteRecords);
        writeData.setCreateTime(new Date());
        if (StringUtils.isNotEmpty(type)) {
            writeData.setType(type);
        }
        homeworkStudentWriteDataService.save(writeData);

        // 更新作业状态
        studentsHomework.setSubmitStatus(1);
        studentsHomework.setSubmitTime(new Date());
        if ("1".equals(type)) {
            studentsHomework.setAuditStatus(1);
        } else if ("2".equals(type)) {
            studentsHomework.setEmendStatus(2);
            studentsHomework.setAuditStatus(4);
        }

        // 保存作业状态
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);

        // 清除缓存
        String cacheKey = "studentsHomework:id:" + studentsHomework.getId();
        redisTemplate.delete(cacheKey);

        // 更新作业发布状态
        HomeworkPublish homeworkPublish = homeworkPublishRepository.findById(studentsHomework.getHomeworkPublishId()).orElse(null);
        if (homeworkPublish != null) {
            homeworkPublish.setAuditStatus(1);
            homeworkPublishRepository.save(homeworkPublish);
        }

        // 异步处理AI智能审批
        if (isFinish) {
            StudentsHomeworkNew finalStudentsHomework = studentsHomework;
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始异步处理AI智能审批，type: {}", type);
                    if ("1".equals(type)) {
                        studentHomeworkAIService.aIauditMid(finalStudentsHomework.getId());
                    } else {
                        studentHomeworkAIService.aIauditMid2(finalStudentsHomework.getId());
                    }
                    log.info("异步处理AI智能审批完成");
                } catch (Exception e) {
                    log.error("AI智能审批异步处理失败", e);
                }
            }).exceptionally(ex -> {
                log.error("异步任务执行异常", ex);
                return null;
            });
        }
    }

    @Override
    public void saveStartTime(Long homeworkId) {
        if(homeworkId==null){
            return;
        }
        studentsHomeworkNewRepository.saveStartTime(homeworkId,new Date());

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
            homeWork2Board.setHomeworkId(homework.getId());
            homeWork2Board.setHomeworkName(homework.getHomeworkPublishName());
            homeWork2Board.setSubject(homework.getSubject());
            if(homework.getTopicImages()!=null&&homework.getTopicImages().size()>0){
                int size = 0;
                for(String url:homework.getTopicImages()){
                    if(url.endsWith(".pdf")){
                        //获取PDF页数
                        int pdfSize = pdfUtil.getPdfPageCount(url);
                        size = size + pdfSize;
                    }else{
                        size++;
                    }
                }
                homeWork2Board.setPageSize(size);
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
            List<StudentsHomeworkCorrect> correctsToSave = new ArrayList<>();
            for(StudentsHomeworkCorrect correct:studentsHomework.getHomeworkCorrectList()){
                correct.setStudentsHomeworkId(studentsHomework.getId());
                correct.setFileType("录音");
                correct.setCreaterType(1);
                correct.setType(1);
                correct.setCreateTime(new Date());
                correctsToSave.add(correct);
            }
            if(!correctsToSave.isEmpty()) {
                studentsHomeworkCorrectRepository.saveAll(correctsToSave);
            }
        }
        if(studentsHomework.getHomeworkCorrectList2()!=null&&!studentsHomework.getHomeworkCorrectList2().isEmpty()){
            List<StudentsHomeworkCorrect> correctsToSave2 = new ArrayList<>();
            for(StudentsHomeworkCorrect correct:studentsHomework.getHomeworkCorrectList2()){
                correct.setStudentsHomeworkId(studentsHomework.getId());
                correct.setFileType("录音");
                correct.setCreaterType(1);
                correct.setType(2);
                correct.setCreateTime(new Date());
                correctsToSave2.add(correct);
            }
            if(!correctsToSave2.isEmpty()) {
                studentsHomeworkCorrectRepository.saveAll(correctsToSave2);
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
                                    // 确保设置学校ID
                                    if (wrongTitleBook.getSchoolId() == null && finalStudentsHomework.getSchoolId() != null) {
                                        wrongTitleBook.setSchoolId(finalStudentsHomework.getSchoolId());
                                    }
                                    // 添加：设置科目信息
                                    if (StringUtils.isEmpty(wrongTitleBook.getSubject()) && StringUtils.isNotEmpty(finalStudentsHomework.getSubject())) {
                                        wrongTitleBook.setSubject(finalStudentsHomework.getSubject());
                                    }
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
                            // 确保设置学校ID
                            if (wrongTitleBook.getSchoolId() == null && finalStudentsHomework.getSchoolId() != null) {
                                wrongTitleBook.setSchoolId(finalStudentsHomework.getSchoolId());
                            }
                            // 添加：设置科目信息
                            if (StringUtils.isEmpty(wrongTitleBook.getSubject()) && StringUtils.isNotEmpty(finalStudentsHomework.getSubject())) {
                                wrongTitleBook.setSubject(finalStudentsHomework.getSubject());
                            }
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

            //System.out.println(futureTask.get()); // 获取结果，会阻塞直到任务完成
        } catch (Exception e) {
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
    public Map<Long, Long> getSubmitNumMapByHomeworkPublishIds(List<Long> homeworkPublishIds) {
        Map<Long, Long> submitNumMap = new HashMap<>();
        if (CollectionUtils.isEmpty(homeworkPublishIds)) {
            return submitNumMap;
        }

        List<Object[]> results = studentsHomeworkNewRepository.countSubmittedByHomeworkPublishIds(homeworkPublishIds);
        for (Object[] result : results) {
            Long homeworkPublishId = (Long) result[0];
            Long count = ((Number) result[1]).longValue();
            submitNumMap.put(homeworkPublishId, count);
        }

        // 确保所有请求的ID都有对应的值（默认0）
        for (Long homeworkPublishId : homeworkPublishIds) {
            submitNumMap.computeIfAbsent(homeworkPublishId, k -> 0L);
        }

        return submitNumMap;
    }
    @Override
    public StudentsHomeworkNew appSubmit(StudentsHomeworkNew studentsHomework) {
        studentsHomework =studentsHomeworkNewRepository.save(studentsHomework);
        StudentsHomeworkNew finalStudentsHomework = studentsHomework;
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            studentHomeworkAIService.appSubmitAI(finalStudentsHomework);
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
            studentHomeworkAIService.appEmendSubmitAI(finalStudentsHomework);
            return "异步-OK";

        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return studentsHomework;
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
