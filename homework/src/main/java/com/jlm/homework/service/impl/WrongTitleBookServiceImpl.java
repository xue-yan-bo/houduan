package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.IWrongTitleBookService;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import com.jlm.homework.util.PiontSignUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public Page<WrongTitleBook> findByStudentId(Long studentId,Integer pageNum,Integer pageSize) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        WrongTitleBook search = new WrongTitleBook();
        search.setStudentId(studentId);
        return wrongTitleBookRepository.findAll(Example.of(search),pageable);
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
        wrongTitleBook.setCreateTime(new Date());
        wrongTitleBookRepository.save(wrongTitleBook);
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
            if(StringUtils.isEmpty(wrongTitleBook.getTitleImage())){
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
}
