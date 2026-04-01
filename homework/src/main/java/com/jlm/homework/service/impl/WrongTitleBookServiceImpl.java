package com.jlm.homework.service.impl;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.IWrongTitleBookService;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import com.jlm.homework.util.AIUtil;
import com.jlm.homework.util.PiontSignUtil;
import com.jlm.homework.util.QianWenAIUtil;
import com.jlm.homework.util.ZhipuAIImageAnalysisUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Service
public class WrongTitleBookServiceImpl implements IWrongTitleBookService {
    @Resource
    private WrongTitleBookRepository wrongTitleBookRepository;
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Resource
    private ExerciseBookQuestionRepository exerciseBookQuestionRepository;
    @Resource
    private WrongTitleStatisticsRepository wrongTitleStatisticsRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private AIUtil aiUtil;
    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        extractImageTextIfEmpty(wrongTitleBook);
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public Page<WrongTitleBook> findByStudentId(Long studentId,Integer pageNum,Integer pageSize,String source,Integer commandFlag) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Specification<WrongTitleBook> specification= new Specification<WrongTitleBook>() {

            @Override
            public Predicate toPredicate(Root<WrongTitleBook> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate cond = criteriaBuilder.equal(root.get("studentId"),studentId);
                    list.add(cond);
                    // 排除掉那些已经被合并掉的题目（状态为2的），只显示主题目和解析中的题目
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 2));
                    if(StringUtils.isNotEmpty(source)){
                        Predicate condition = criteriaBuilder.like(root.get("source"),"%"+source+"%");
                        list.add(condition);
                    }
                    if(commandFlag!=null){
                        Predicate condition1 = criteriaBuilder.equal(root.get("commandFlag"),commandFlag);
                        list.add(condition1);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return wrongTitleBookRepository.findAll(specification,pageable);
    }

    public void createWrongBook(Long studentsHomeworkId){
        try {
            StudentsHomeworkNew studentsHomework=studentsHomeworkNewRepository.findById(studentsHomeworkId).get();
            HomeworkPublish homeworkPublish = homeworkPublishRepository.findById(studentsHomework.getHomeworkPublishId()).get();
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
                                    // 给刚创建的错题默认加上 duplicateStatus = 0 (解析中)，以便前端识别
                                    wrongTitleBook.setDuplicateStatus(0);

                                    // 先保存第一遍，拿到 ID 并且让数据库里有一条"解析中"的记录
                                    wrongTitleBookRepository.save(wrongTitleBook);
                                    // 随后立刻提取文本(如果需要)并跑 AI 分析和查重
                                    extractImageTextIfEmpty(wrongTitleBook);
                                    wrongTitleBookRepository.save(wrongTitleBook);
                                    aiChart(wrongTitleBook.getId());
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
                            // 给刚创建的错题默认加上 duplicateStatus = 0 (解析中)，以便前端识别
                            wrongTitleBook.setDuplicateStatus(0);

                            // 先保存第一遍，拿到 ID 并且让数据库里有一条"解析中"的记录
                            wrongTitleBookRepository.save(wrongTitleBook);
                            // 随后立刻提取文本(如果需要)并跑 AI 分析和查重
                            extractImageTextIfEmpty(wrongTitleBook);
                            wrongTitleBookRepository.save(wrongTitleBook);
                            aiChart(wrongTitleBook.getId());
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
    }

    @Override
    public Result<Object> addWrongBook(WrongTitleBook wrongTitleBook) {
        boolean isNew = true;
        if (wrongTitleBook.getQuestionId() != null && wrongTitleBook.getStudentId() != null) {
            WrongTitleBook search = new WrongTitleBook();
            search.setQuestionId(wrongTitleBook.getQuestionId());
            search.setStudentId(wrongTitleBook.getStudentId());
            // TODO: 修改为 findAll 来避免数据库脏数据导致的 500 报错
            List<WrongTitleBook> list = wrongTitleBookRepository.findAll(Example.of(search));
            if (list != null && !list.isEmpty()) {
                WrongTitleBook soWrongTitle = list.get(0); // 取第一条即可
                wrongTitleBook.setId(soWrongTitle.getId());
                isNew = false;
            }
        }
        if (wrongTitleBook.getSchoolId() == null && wrongTitleBook.getStudentId() != null) {
            ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(wrongTitleBook.getStudentId());
            if (resultDto != null && resultDto.getData() != null) {
                wrongTitleBook.setSchoolId(resultDto.getData().getSchoolId());
            }
        }
        extractImageTextIfEmpty(wrongTitleBook);
        wrongTitleBook.setCreateTime(new Date());
        if(StringUtils.isEmpty(wrongTitleBook.getSource())){
            wrongTitleBook.setSource("学生自加");
        }
        if(isNew) {
            wrongTitleBook.setDuplicateStatus(0); // 刚添加的标记为0解析中
        }
        wrongTitleBookRepository.save(wrongTitleBook);
        if(!isNew){
            return Result.success("该错题已存在");
        }
        if(wrongTitleBook.getStudentsHomeworkId()!=null) {
            Optional<StudentsHomeworkNew> optional = studentsHomeworkNewRepository.findById(wrongTitleBook.getStudentsHomeworkId());
            if (optional != null && optional.isPresent()) {
                StudentsHomeworkNew homeworkNew = optional.get();
                if (StringUtils.isEmpty(wrongTitleBook.getSubject()) && StringUtils.isNotEmpty(homeworkNew.getSubject())) {
                    wrongTitleBook.setSubject(homeworkNew.getSubject());
                    wrongTitleBookRepository.save(wrongTitleBook);
                }
                if (homeworkNew.getAccuracy() == null) {
                    homeworkNew.setAccuracy(98.0);
                } else {
                    homeworkNew.setAccuracy(homeworkNew.getAccuracy() - 2);
                }
                studentsHomeworkNewRepository.save(homeworkNew);
            }
            this.addClassWrongTitle(wrongTitleBook);
        }
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            aiChart(wrongTitleBook.getId());
            return "异步-OK";
        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return Result.success("错题添加成功，后台正在解析并查重", java.util.Collections.singletonList(wrongTitleBook));
    }

    private void addClassWrongTitle(WrongTitleBook wrongTitleBook) {
        Integer studentNum = 0;
        Long schoolId = null;
        ResultDto<Student> resultDto= studentFeignClient.getStudentInfo(wrongTitleBook.getStudentId());
        if(resultDto!=null&&resultDto.getData()!=null){
            schoolId = resultDto.getData().getSchoolId();
        }
        if(wrongTitleBook.getClassId()!=null&&schoolId!=null){
            Result<Student> result = studentFeignClient.getStudentList(1,200,schoolId,null,wrongTitleBook.getClassId(),"0");
            if(result!=null&&result.getRows()!=null&&result.getRows().size()>0){
                studentNum =result.getRows().size();
            }
        }

        WrongTitleStatistics search =  new WrongTitleStatistics();
        search.setHomeworkPublishId(wrongTitleBook.getHomeworkPublishId());
        search.setClassId(wrongTitleBook.getClassId());
        search.setTitleBigNo(wrongTitleBook.getTitleBigNo());
        search.setTitleSmallNo(wrongTitleBook.getTitleSmallNo());
        Optional<WrongTitleStatistics> optional=wrongTitleStatisticsRepository.findOne(Example.of(search));
        if(optional!=null&&optional.isPresent()){
            WrongTitleStatistics wrongTitleStatistics = optional.get();
            Integer wrongStudentNum = 0;
            if(wrongTitleStatistics.getWrongStudentNum()!=null) {
                wrongStudentNum = wrongTitleStatistics.getWrongStudentNum() + 1;
                wrongTitleStatistics.setWrongStudentNum(wrongStudentNum);
            }
            if(studentNum!=0){
                Double wrongRate = BigDecimal.valueOf(wrongStudentNum).divide(BigDecimal.valueOf(studentNum), 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                wrongTitleStatistics.setWrongRate(wrongRate);
            }

            wrongTitleStatisticsRepository.save(wrongTitleStatistics);
        }else{
            WrongTitleStatistics newWrongTitle = new WrongTitleStatistics();
            newWrongTitle.setHomeworkPublishId(wrongTitleBook.getHomeworkPublishId());
            newWrongTitle.setHomeworkPublishName(wrongTitleBook.getHomeworkPublishName());
            newWrongTitle.setClassId(wrongTitleBook.getClassId());
            newWrongTitle.setSchoolId(wrongTitleBook.getSchoolId());
            newWrongTitle.setSubject(wrongTitleBook.getSubject());
            newWrongTitle.setQuestionId(wrongTitleBook.getQuestionId());
            newWrongTitle.setTitleBigNo(wrongTitleBook.getTitleBigNo());
            newWrongTitle.setTitleSmallNo(wrongTitleBook.getTitleSmallNo());
            newWrongTitle.setSource("作业");
            if(StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())){
                newWrongTitle.setTitleImage(wrongTitleBook.getTitleImage());
            }else {
                newWrongTitle.setTitleImage(wrongTitleBook.getSourceImageUrl());
            }
            newWrongTitle.setParse(wrongTitleBook.getParse());
            newWrongTitle.setPageNo(wrongTitleBook.getPageNo());
            if(StringUtils.isNotEmpty(wrongTitleBook.getTitleContext())&&
                wrongTitleBook.getTitleContext().contains("\\")){
                newWrongTitle.setTitleContext(wrongTitleBook.getTitleContext().replace("\\",""));
            }else {
                newWrongTitle.setTitleContext(wrongTitleBook.getTitleContext());
            }
            newWrongTitle.setWrongStudentNum(1);
            if(studentNum!=0){
                Double wrongRate = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(studentNum), 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                newWrongTitle.setWrongRate(wrongRate);
            }
            newWrongTitle.setCreateDate(new Date());
            wrongTitleStatisticsRepository.save(newWrongTitle);
        }
    }
    @Override
    public Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        if(wrongTitleBook!=null){
            if(StringUtils.isEmpty(wrongTitleBook.getHomeworkPublishName())){
                wrongTitleBook.setHomeworkPublishName(null);
            }
            if(StringUtils.isEmpty(wrongTitleBook.getSource())){
                wrongTitleBook.setSource(null);
            }
        }
        return wrongTitleBookRepository.findAll(Example.of(wrongTitleBook),pageable);
    }

    @Override
    public void aiChart(Long wrongTitleId) {
        Optional<WrongTitleBook> optional=wrongTitleBookRepository.findById(wrongTitleId);
        if(optional==null||!optional.isPresent()){
            return;
        }
        WrongTitleBook wrongTitleBook = optional.get();
        if (StringUtils.isEmpty(wrongTitleBook.getTitleContext())) {
            String imageUrl = null;
            if (StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                imageUrl = wrongTitleBook.getTitleImage();
            } else if (StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                imageUrl = wrongTitleBook.getSourceImageUrl();
            }
            if (StringUtils.isNotEmpty(imageUrl)) {
                try {
                    AIUtil util = aiUtil.getAIUtil();
                    String prompt = "请提取这张图片中的所有试题文字内容（包含题目、选项、解析等）。不论是文科（语文、历史、英语等）、理科还是美术等其他学科，请忠实还原图片中的所有文字。如果包含公式或特殊符号，请尽量使用Markdown或LaTeX语法表示。只返回提取的纯文字内容，不要输出诸如好的、提取的文字如下等任何废话。如果识别不到文字，只需返回空字符串。";
                    String text = util.analyzeImage(imageUrl, prompt);
                    if (StringUtils.isNotEmpty(text)) {
                        wrongTitleBook.setTitleContext(text);
                        // ----------- 新增：异步去重查重核心逻辑 -----------
                        // 查询范围扩大到当前班级 (不仅限当前学生)
                        if (wrongTitleBook.getClassId() != null) {
                            WrongTitleBook search = new WrongTitleBook();
                            search.setClassId(wrongTitleBook.getClassId());

                            // 查找该班级所有的历史错题
                            java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(Example.of(search));

                            double maxSimilarity = 0.0;
                            WrongTitleBook mostSimilarBook = null;

                            // 遍历比对相似度
                            for (WrongTitleBook history : historyBooks) {
                                // 排除自己，且排除没有文本内容的记录
                                if (history.getId().equals(wrongTitleBook.getId()) || StringUtils.isEmpty(history.getTitleContext())) {
                                    continue;
                                }
                                double similarity = com.jlm.homework.util.TextSimilarityUtil.getSimilarity(text, history.getTitleContext());
                                if (similarity > maxSimilarity) {
                                    maxSimilarity = similarity;
                                    mostSimilarBook = history;
                                }
                            }

                            // 根据最高相似度打标记
                            if (mostSimilarBook != null && maxSimilarity > 0.80) {
                                // 相似度 > 80%，直接合并废弃当前题
                                wrongTitleBook.setDuplicateStatus(2); // 2=已合并废弃
                                wrongTitleBook.setDuplicateOf(mostSimilarBook.getId());

                                // 把历史那道老题目的错误次数 + 1
                                Integer oldErrorCount = mostSimilarBook.getErrorCount() == null ? 1 : mostSimilarBook.getErrorCount();
                                mostSimilarBook.setErrorCount(oldErrorCount + 1);
                                // 提前保存老题目的修改
                                wrongTitleBookRepository.save(mostSimilarBook);
                            } else {
                                // 其他情况（<=80% 或者没有历史题），视为全新题，标记为 3（解析完成的新题）
                                wrongTitleBook.setDuplicateStatus(3); // 3=解析完成的全新题
                            }
                        } else if (wrongTitleBook.getStudentId() != null) {
                            // 降级策略：如果没有班级ID，至少按学生个人查重
                            WrongTitleBook search = new WrongTitleBook();
                            search.setStudentId(wrongTitleBook.getStudentId());
                            java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(Example.of(search));

                            double maxSimilarity = 0.0;
                            WrongTitleBook mostSimilarBook = null;

                            for (WrongTitleBook history : historyBooks) {
                                if (history.getId().equals(wrongTitleBook.getId()) || StringUtils.isEmpty(history.getTitleContext())) {
                                    continue;
                                }
                                double similarity = com.jlm.homework.util.TextSimilarityUtil.getSimilarity(text, history.getTitleContext());
                                if (similarity > maxSimilarity) {
                                    maxSimilarity = similarity;
                                    mostSimilarBook = history;
                                }
                            }

                            if (mostSimilarBook != null && maxSimilarity > 0.80) {
                                wrongTitleBook.setDuplicateStatus(2); // 2=已合并废弃
                                wrongTitleBook.setDuplicateOf(mostSimilarBook.getId());
                                Integer oldErrorCount = mostSimilarBook.getErrorCount() == null ? 1 : mostSimilarBook.getErrorCount();
                                mostSimilarBook.setErrorCount(oldErrorCount + 1);
                                wrongTitleBookRepository.save(mostSimilarBook);
                            } else {
                                wrongTitleBook.setDuplicateStatus(3); // 3=解析完成的全新题
                            }
                        }
                        // ----------- 去重逻辑结束 -----------

                        // 保存最终提取和比对状态后的新题
                        wrongTitleBookRepository.save(wrongTitleBook);
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }
    }



    @Override
    public void updateCommandFlag(Long wrongTitleId, Integer commandFlag) {
        Optional<WrongTitleBook> optional=wrongTitleBookRepository.findById(wrongTitleId);
        if(optional.isPresent()){
            WrongTitleBook wrongTitleBook=optional.get();
            wrongTitleBook.setCommandFlag(commandFlag);
            wrongTitleBookRepository.save(wrongTitleBook);
        }
    }

    @Override
    public void updateWrongBook(WrongTitleBook wrongTitleBook) {
        /*if(wrongTitleBook.getSource().contains("作业")){
            throw new RuntimeException("错题来源为作业的，不可以修改！");
        }*/
        WrongTitleBook old=wrongTitleBookRepository.findById(wrongTitleBook.getId()).orElse(null);
        if(StringUtils.isNotEmpty(wrongTitleBook.getTitleContext())){
            old.setTitleContext(wrongTitleBook.getTitleContext());
        }
        if(StringUtils.isNotEmpty(wrongTitleBook.getKnowledgePoint())){
            old.setKnowledgePoint(wrongTitleBook.getKnowledgePoint());
        }
        if(StringUtils.isNotEmpty(wrongTitleBook.getParse())){
            old.setParse(wrongTitleBook.getParse());
        }
        if(StringUtils.isNotEmpty(wrongTitleBook.getTitleAnswer())){
            old.setTitleAnswer(wrongTitleBook.getTitleAnswer());
        }
        if(StringUtils.isNotEmpty(wrongTitleBook.getStudentAnswer())){
            old.setStudentAnswer(wrongTitleBook.getStudentAnswer());
        }
        if(StringUtils.isNotEmpty(wrongTitleBook.getStudentAnswer())){
            old.setStudentAnswer(wrongTitleBook.getStudentAnswer());
        }
        wrongTitleBookRepository.save(old);
    }

    @Override
    public void confirmDuplicate(Long id) {
        Optional<WrongTitleBook> optional = wrongTitleBookRepository.findById(id);
        if (optional.isPresent()) {
            WrongTitleBook wrongTitleBook = optional.get();
            if (wrongTitleBook.getDuplicateStatus() != null && wrongTitleBook.getDuplicateStatus() == 1 && wrongTitleBook.getDuplicateOf() != null) {
                // 将疑似重复标记为确定废弃
                wrongTitleBook.setDuplicateStatus(2);

                // 更新母题的错误次数
                Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(wrongTitleBook.getDuplicateOf());
                if (parentOpt.isPresent()) {
                    WrongTitleBook parentBook = parentOpt.get();
                    Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                    parentBook.setErrorCount(oldErrorCount + 1);
                    wrongTitleBookRepository.save(parentBook);
                }
                wrongTitleBookRepository.save(wrongTitleBook);
            }
        }
    }

    @Override
    public void rejectDuplicate(Long id) {
        Optional<WrongTitleBook> optional = wrongTitleBookRepository.findById(id);
        if (optional.isPresent()) {
            WrongTitleBook wrongTitleBook = optional.get();
            if (wrongTitleBook.getDuplicateStatus() != null && wrongTitleBook.getDuplicateStatus() == 1) {
                // 拒绝合并，作为全新的错题
                wrongTitleBook.setDuplicateStatus(0);
                wrongTitleBook.setDuplicateOf(null);
                wrongTitleBookRepository.save(wrongTitleBook);
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        wrongTitleBookRepository.deleteById(id);
    }

    private void extractImageTextIfEmpty(WrongTitleBook wrongTitleBook) {
        if (StringUtils.isEmpty(wrongTitleBook.getTitleContext())) {
            String imageUrl = null;
            if (StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                imageUrl = wrongTitleBook.getTitleImage();
            } else if (StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                imageUrl = wrongTitleBook.getSourceImageUrl();
            }

            if (StringUtils.isNotEmpty(imageUrl)) {
                try {
                    AIUtil util = aiUtil.getAIUtil();
                    String prompt = "请提取这张图片中的所有试题文字内容（包含题目、选项、解析等）。不论是文科（语文、历史、英语等）、理科还是美术等其他学科，请忠实还原图片中的所有文字。如果包含公式或特殊符号，请尽量使用Markdown或LaTeX语法表示。只返回提取的纯文字内容，不要输出诸如'好的'、'提取的文字如下'等任何废话。如果识别不到文字，只需返回空字符串。";
                    String text = util.analyzeImage(imageUrl, prompt);
                    if (StringUtils.isNotEmpty(text)) {
                        wrongTitleBook.setTitleContext(text);
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }
    }
}
