package com.jlm.homework.service.impl;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.IWrongTitleBookService;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import com.jlm.homework.util.PiontSignUtil;
import com.jlm.homework.util.ZhipuAIImageAnalysisUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
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
    private ZhipuAIConfig zhipuAIConfig;
    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public Page<WrongTitleBook> findByStudentId(Long studentId,Integer pageNum,Integer pageSize,String source,Integer commandFlag) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Specification<WrongTitleBook> specification= new Specification<WrongTitleBook>() {

            @Override
            public Predicate toPredicate(Root<WrongTitleBook> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate cond = criteriaBuilder.equal(root.get("studentId"),studentId);
                    list.add(cond);
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
                                    wrongTitleBook.setTitleImage(question.getCroppedUrl());
                                    wrongTitleBook.setSourceImageUrl(question.getSourceImageUrl());
                                    wrongTitleBook.setTitleBigNo(question.getTitleBigNo());
                                    wrongTitleBook.setTitleSmallNo(question.getTitleSmallNo());
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
                            wrongTitleBook.setTitleImage(question.getCroppedUrl());
                            wrongTitleBook.setSourceImageUrl(question.getSourceImageUrl());
                            wrongTitleBook.setTitleBigNo(question.getTitleBigNo());
                            wrongTitleBook.setTitleSmallNo(question.getTitleSmallNo());
                            wrongTitleBookRepository.save(wrongTitleBook);
                            aiChart(wrongTitleBook.getId());
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
    }

    @Override
    public void addWrongBook(WrongTitleBook wrongTitleBook) {
        boolean isNew = true;
        if(StringUtils.isNotEmpty(wrongTitleBook.getTitleBigNo())&&StringUtils.isNotEmpty(wrongTitleBook.getTitleSmallNo())){
            WrongTitleBook search = new WrongTitleBook();
            search.setTitleBigNo(wrongTitleBook.getTitleBigNo());
            search.setTitleSmallNo(wrongTitleBook.getTitleSmallNo());
            search.setStudentsHomeworkId(wrongTitleBook.getStudentsHomeworkId());
            Optional<WrongTitleBook> soWrongTitle = wrongTitleBookRepository.findOne(Example.of(search));
            if(soWrongTitle!=null&&soWrongTitle.isPresent()){
                wrongTitleBook.setId(soWrongTitle.get().getId());
                isNew = false;
            }
        }
        wrongTitleBook.setCreateTime(new Date());
        wrongTitleBookRepository.save(wrongTitleBook);
        if(!isNew){
            return;
        }
        Optional<StudentsHomeworkNew> optional=studentsHomeworkNewRepository.findById(wrongTitleBook.getStudentsHomeworkId());
        if(optional!=null&&optional.isPresent()){
            StudentsHomeworkNew homeworkNew = optional.get();
            if(homeworkNew.getAccuracy()==null){
                homeworkNew.setAccuracy(98.0);
            }else{
                homeworkNew.setAccuracy(homeworkNew.getAccuracy()-2);
            }
            studentsHomeworkNewRepository.save(homeworkNew);
        }

        this.addClassWrongTitle(wrongTitleBook);
        FutureTask<String> futureTask = new FutureTask<>(() -> {

            aiChart(wrongTitleBook.getId());
            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
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
                Double wrongRate = BigDecimal.valueOf(wrongStudentNum).divide(BigDecimal.valueOf(studentNum),4,BigDecimal.ROUND_HALF_UP)
                        .doubleValue();
                wrongTitleStatistics.setWrongRate(wrongRate);
            }

            wrongTitleStatisticsRepository.save(wrongTitleStatistics);
        }else{
            WrongTitleStatistics newWrongTitle = new WrongTitleStatistics();
            newWrongTitle.setHomeworkPublishId(wrongTitleBook.getHomeworkPublishId());
            newWrongTitle.setHomeworkPublishName(wrongTitleBook.getHomeworkPublishName());
            newWrongTitle.setClassId(wrongTitleBook.getClassId());
            newWrongTitle.setQuestionId(wrongTitleBook.getQuestionId());
            newWrongTitle.setTitleBigNo(wrongTitleBook.getTitleBigNo());
            newWrongTitle.setTitleSmallNo(wrongTitleBook.getTitleSmallNo());
            newWrongTitle.setSource("作业");
            newWrongTitle.setTitleImage(wrongTitleBook.getTitleImage());
            if(StringUtils.isEmpty(wrongTitleBook.getSourceImageUrl())){
                newWrongTitle.setTitleImage(wrongTitleBook.getSourceImageUrl());
            }
            newWrongTitle.setParse(wrongTitleBook.getParse());
            newWrongTitle.setPageNo(wrongTitleBook.getPageNo());
            newWrongTitle.setTitleContext(wrongTitleBook.getTitleContext());
            newWrongTitle.setWrongStudentNum(1);
            if(studentNum!=0){
                Double wrongRate = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(studentNum),4,BigDecimal.ROUND_HALF_UP)
                        .doubleValue();
                newWrongTitle.setWrongRate(wrongRate);
            };
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

        ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();

        try {
            String  resultStr = null;
            if(StringUtils.isNotEmpty(wrongTitleBook.getTitleContext())){
                String prompt = "根据题目内容："+wrongTitleBook.getTitleContext() +"     分析该题的知识考点以及生成知识考点的图谱(图谱呈现父子节点json格式)。返回格式如： 知识点：   知识图谱：{\"父节点\":{\"名称\":\" \",\"阐述\":\" \",\"子节点\":[{\"名称\":\" \",\"阐述\":\" \"},{\"名称\":\" \",\"阐述\":\" \", \"子节点\":[{\"名称\":\" \",\"阐述\":\" \"}] }]}} ";
                resultStr =util.analyzeToJson(prompt);

            }else if(StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())){
                String prompt = "根据题图片，分析该题的知识考点以及生成知识考点的图谱(图谱呈现父子节点json格式)。返回格式如： 知识点：   知识图谱：{\"父节点\":{\"名称\":\" \",\"阐述\":\" \",\"子节点\":[{\"名称\":\" \",\"阐述\":\" \"},{\"名称\":\" \",\"阐述\":\" \", \"子节点\":[{\"名称\":\" \",\"阐述\":\" \"}] }]}} ";
                resultStr=util.analyzeImageToJson(wrongTitleBook.getSourceImageUrl(),prompt);
            }else if(StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())){
                String prompt = "根据题图片，分析该题的知识考点以及生成知识考点的图谱(图谱呈现父子节点json格式)。返回格式如： 知识点：   知识图谱：{\"父节点\":{\"名称\":\" \",\"阐述\":\" \",\"子节点\":[{\"名称\":\" \",\"阐述\":\" \"},{\"名称\":\" \",\"阐述\":\" \", \"子节点\":[{\"名称\":\" \",\"阐述\":\" \"}] }]}} ";
                resultStr=util.analyzeImageToJson(wrongTitleBook.getTitleImage(),prompt);
            }

            System.out.println("AI分析结果: " + resultStr);
            Map<String, Object> resultMap = JSONObject.parseObject(resultStr);
            String content = resultMap.get("content").toString();
            if(StringUtils.isNotEmpty(content)&&resultStr.contains("知识点")&&resultStr.contains("知识图谱")){
                String knowledgePoint = content.substring(content.indexOf("知识点")+4,content.indexOf("知识图谱"));
                String aiChart = content.substring(content.lastIndexOf("知识图谱")+5);
                if(aiChart.contains("<|end_of_box|>")){
                    aiChart = aiChart.substring(0,aiChart.indexOf("<|end_of_box|>"));
                }
                if(aiChart.contains("<|begin_of_box|>")&&!aiChart.startsWith("<|begin_of_box|>")){
                    aiChart = aiChart.substring(0,aiChart.indexOf("<|begin_of_box|>"));
                }
                aiChart=aiChart.replaceAll("\\\\", "");
                aiChart=aiChart.replace("\\n","");
                JSONObject json = JSON.parseObject(aiChart);
                wrongTitleBook.setKnowledgePoint(knowledgePoint);
                wrongTitleBook.setAiChart(json);
                wrongTitleBookRepository.save(wrongTitleBook);
                WrongTitleStatistics search = new WrongTitleStatistics();
                search.setHomeworkPublishId(wrongTitleBook.getHomeworkPublishId());
                search.setClassId(wrongTitleBook.getClassId());
                search.setTitleBigNo(wrongTitleBook.getTitleBigNo());
                search.setTitleSmallNo(wrongTitleBook.getTitleSmallNo());
                wrongTitleStatisticsRepository.findOne(Example.of(search)).ifPresent(
                        wrongTitleStatistics->{
                            wrongTitleStatistics.setKnowledgePoint(knowledgePoint);
                            wrongTitleStatistics.setAiChart(json);
                            wrongTitleStatisticsRepository.save(wrongTitleStatistics);
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
}
