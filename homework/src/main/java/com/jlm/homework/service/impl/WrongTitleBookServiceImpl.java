package com.jlm.homework.service.impl;

import com.jlm.homework.entity.*;
import com.jlm.homework.repository.ExerciseBookQuestionRepository;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.repository.WrongTitleBookRepository;
import com.jlm.homework.service.IWrongTitleBookService;
import com.jlm.homework.util.PiontSignUtil;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public List<WrongTitleBook> findByStudentId(Long studentId) {
        WrongTitleBook search = new WrongTitleBook();
        search.setStudentId(studentId);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        return wrongTitleBookRepository.findAll(Example.of(search),sort);
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
        wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return wrongTitleBookRepository.findAll(Example.of(wrongTitleBook),pageable);
    }
}
