package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson2.JSONObject;
import com.jlm.homework.config.RabbitMQConfig;
import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.dto.HomeWork2Board;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.ClassFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.service.*;
import com.jlm.homework.repository.WrongGroupRepository;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.FutureTask;

@Slf4j
@Service
public class HandlerServiceImpl implements IHandlerService {

    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Autowired
    private IStudentFeedbackService studentFeedbackService;
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;
    @Autowired
    private IWrongTitleWriteDataService wrongTitleWriteDataService;
    @Autowired
    private ICopybookStudentWriteDataService copybookStudentWriteDataService;
    @Autowired
    private ICopybookStudentRecordService copybookStudentRecordService;

    @Autowired
    private StudentFeignClient studentFeignClient;

    @Autowired
    private ClassFeignClient classFeignClient;
    @Autowired
    private AIUtil aiUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WrongGroupRepository wrongGroupRepository;

    @Override
    public List<HomeWork2Board> getHomeWork2Board(String subject, String date, Long studentId) {
        return studentsHomeworkNewService.getHomeWork2Board(subject,date,studentId);
    }


    @Resource
    private RabbitMessagingTemplate mqTemplate;

    @Override
    public void saveWriteRecords(Long studentId, Long homeworkId, String type, Integer pageN, List<StudentsWriteRecord> studentsWriteRecords, Boolean isFinish) {
        System.out.println("开始保存笔记");
        studentsHomeworkNewService.saveWriteRecords(studentId,homeworkId,type,pageN,studentsWriteRecords,isFinish);
        /*if(isFinish){
            // 异步发送消息到MQ，避免阻塞处理线程
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // 延时5秒后发送消息到MQ
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.warn("Sleep interrupted while waiting to send MQ message", e);
                    Thread.currentThread().interrupt();
                }
                // 发送消息到MQ
                JSONObject json= new JSONObject();
                json.put("studentId", studentId);
                json.put("homeworkId", homeworkId);
                json.put("type", type);
                try {
                    mqTemplate.convertAndSend(RabbitMQConfig.HOMEWORK_CORRECTION_QUEUE, json.toJSONString());
                } catch (Exception e) {
                    log.error("Failed to send MQ message", e);
                }
            });
        }*/

    }

    @Override
    public void saveStartTime(Long homeworkId) {
        studentsHomeworkNewService.saveStartTime(homeworkId);
    }

    @Override
    public List<HomeWork2Board> getEmendHomeWork2Board(String subject, Long studentId) {
        return studentsHomeworkNewService.getEmendHomeWork2Board(subject,studentId);
    }

    @Override
    public Long saveFeedbackRecords(Long feedbackId,Long studentId, String subject, List<StudentsWriteRecord> studentsFeedbackRecords) {
        StudentFeedback feedback = null;
        if(feedbackId!=null) {
            feedback = studentFeedbackService.findById(feedbackId);
        }
        if(studentsFeedbackRecords==null||studentsFeedbackRecords.isEmpty()){
            return feedbackId;
        }
        Date now = new Date();
        if(feedback==null) {
            feedback = new StudentFeedback();
            feedback.setCreateTime(now);
            feedback.setStudentId(studentId);
            
            // 先从Redis获取学生信息
            String redisKey = "student:info:" + studentId;
            Student student = null;
            try {
                Object cachedStudent = redisTemplate.opsForValue().get(redisKey);
                if (cachedStudent != null) {
                    student = (Student) cachedStudent;
                    log.info("从Redis获取学生信息：id" + student.getStudentId() + "姓名：" + student.getStudentName());
                } else {
                    // Redis中没有，调用feign接口获取
                    ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
                    if(resultDto!=null&&resultDto.getData()!=null) {
                        student = resultDto.getData();
                        // 将学生信息存入Redis，过期时间24小时
                        redisTemplate.opsForValue().set(redisKey, student, 24, TimeUnit.HOURS);
                        //log.info("从Feign获取学生信息并缓存到Redis：id" + student.getStudentId() + "姓名：" + student.getStudentName());
                    }
                }
            } catch (Exception e) {
                log.error("Redis操作失败，尝试从Feign获取学生信息：" + e.getMessage());
                // Redis操作失败，直接调用feign接口
                ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
                if(resultDto!=null&&resultDto.getData()!=null) {
                    student = resultDto.getData();
                    log.info("从Feign获取学生信息：id" + student.getStudentId() + "姓名：" + student.getStudentName());
                }
            }
            
            if(student != null) {
                feedback.setSchoolId(student.getSchoolId());
                feedback.setStudentName(student.getStudentName());
                feedback.setClassId(student.getClassesId());
                if (StringUtils.isEmpty(student.getClassesName()) && student.getClassesId() != null) {
                    Classes classes = classFeignClient.getClasses(student.getClassesId());
                    feedback.setClassName(classes.getName());
                } else {
                    feedback.setClassName(student.getClassesName());
                }
            }
        }

        if(feedbackId!=null&&feedback!=null){
            // 当feedbackId不为空且feedback存在时，只保存到StudentFeedBackWriteData表
            studentFeedbackService.saveMoreWriteRecords(feedbackId,studentId,studentsFeedbackRecords);
        }else {
            // 当feedbackId为空或feedback不存在时，创建新的StudentFeedback记录
            if(feedback.getFeedbackContent()!=null&&!feedback.getFeedbackContent().isEmpty()){
                List<StudentsWriteRecord> writeRecords = feedback.getFeedbackContent();
                writeRecords.addAll(studentsFeedbackRecords);
                feedback.setFeedbackContent(writeRecords);
            }else {
                feedback.setFeedbackContent(studentsFeedbackRecords);
            }
            feedback.setId(feedbackId);
            feedback.setFeedbackTime(now);
            feedback =studentFeedbackService.save(feedback);
        }

        log.info("--------完成反馈信息保存:-------"+feedback.getId());
        return feedback.getId();
    }

    @Override
    public Long saveErrorTitleRecords(Long wrongTitleId, Long studentId, String subject, List<StudentsWriteRecord> uploadErrorTitleRecords) {
        WrongTitleWriteData wrongTitleWriteData = null;
        if(wrongTitleId!=null){
            wrongTitleWriteData = wrongTitleWriteDataService.getById(wrongTitleId);
        }
        if(wrongTitleWriteData==null) {
            wrongTitleWriteData = new WrongTitleWriteData();
            wrongTitleWriteData.setCreateTime(new Date());
        }
        wrongTitleWriteData.setStudentId(studentId);
        wrongTitleWriteData.setSubject(subject);
        wrongTitleWriteData.setStudentsWriteRecords(uploadErrorTitleRecords);

        wrongTitleWriteDataService.save(wrongTitleWriteData);

        WrongTitleWriteData finalWrongTitleWriteData = wrongTitleWriteData;
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
                //System.out.println("错题分析: " + wrongTitleStr);
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
                        wrongTitleBook.setWriteDataId(finalWrongTitleWriteData.getId());
                        wrongTitleBook.setStudentId(studentId);
                        if(resultDto!=null&&resultDto.getData()!=null) {
                            Student student = resultDto.getData();
                            wrongTitleBook.setStudentName(student.getStudentName());
                            wrongTitleBook.setClassId(student.getClassesId());
                            wrongTitleBook.setClassName(student.getClassesName());
                            if (wrongTitleBook.getSchoolId() == null && student.getSchoolId() != null) {
                                wrongTitleBook.setSchoolId(student.getSchoolId());
                            }
                            if (StringUtils.isEmpty(wrongTitleBook.getSubject()) && StringUtils.isNotEmpty(finalWrongTitleWriteData.getSubject())) {
                                wrongTitleBook.setSubject(finalWrongTitleWriteData.getSubject());
                            }
                        }
                        wrongTitleBookService.addWrongBook(wrongTitleBook);
                    }
                }
                File file = new File(imageUrl);
                file.delete();
            } catch (Exception e) {
                //System.out.println("+++++解析分数错误++++++++++ "+e.getMessage() );
            }



            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return wrongTitleWriteData.getId();
    }

    @Override
    public void saveStudentsCopybookRecords(Long studentId, Long recordId, Integer pageN, List<StudentsWriteRecord> studentsCopybookRecords, Boolean isFinish) {
        CopybookStudentRecord record=copybookStudentRecordService.getById(recordId);
        CopybookStudentWriteData writeData = new CopybookStudentWriteData();
        writeData.setStudentId(studentId);
        writeData.setStudentName(record.getStudentName());
        writeData.setStudentRecordId(recordId);
        writeData.setPageNum(pageN);
        writeData.setStudentsWriteRecords(studentsCopybookRecords);
        writeData.setCreateTime(new Date());
        copybookStudentWriteDataService.save(writeData);
        if(isFinish){
            record.setSubmitStatus(1);
            record.setSubmitTime(new Date());
            copybookStudentRecordService.update(record);
        }
    }

    @Override
    public List<Copybook2Board> getCopybookBoards(Long studentId) {
        return copybookStudentRecordService.getCopybookBoards(studentId);
    }

    @Override
    public Long createFeedbackRecords(long studentId) {
        StudentFeedback feedback = new StudentFeedback();
        feedback.setCreateTime(new Date());
        feedback.setStudentId(studentId);
        //feedback.setSubject(subject);
        
        // 先从Redis获取学生信息
        String redisKey = "student:info:" + studentId;
        Student student = null;
        try {
            Object cachedStudent = redisTemplate.opsForValue().get(redisKey);
            if (cachedStudent != null) {
                student = (Student) cachedStudent;
                //log.info("从Redis获取学生信息：id" + student.getStudentId() + "姓名：" + student.getStudentName());
            } else {
                // Redis中没有，调用feign接口获取
                ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
                if(resultDto!=null&&resultDto.getData()!=null) {
                    student = resultDto.getData();
                    // 将学生信息存入Redis，过期时间24小时
                    redisTemplate.opsForValue().set(redisKey, student, 24, TimeUnit.HOURS);
                    //log.info("从Feign获取学生信息并缓存到Redis：id" + student.getStudentId() + "姓名：" + student.getStudentName());
                }
            }
        } catch (Exception e) {
            log.error("Redis操作失败，尝试从Feign获取学生信息：" + e.getMessage());
            // Redis操作失败，直接调用feign接口
            ResultDto<Student> resultDto = studentFeignClient.getStudentInfo(studentId);
            if(resultDto!=null&&resultDto.getData()!=null) {
                student = resultDto.getData();
                log.info("从Feign获取学生信息：id" + student.getStudentId() + "姓名：" + student.getStudentName());
            }
        }
        
        if(student != null) {
            feedback.setSchoolId(student.getSchoolId());
            feedback.setStudentName(student.getStudentName());
            feedback.setClassId(student.getClassesId());
            if (StringUtils.isEmpty(student.getClassesName()) && student.getClassesId() != null) {
                Classes classes = classFeignClient.getClasses(student.getClassesId());
                feedback.setClassName(classes.getName());
            } else {
                feedback.setClassName(student.getClassesName());
            }
            log.info("学生信息：id" + student.getStudentId() + "姓名：" + student.getStudentName());
            feedback =studentFeedbackService.save(feedback);
            return feedback.getId();
        }
        return null;
    }

    @Override
    public List<com.jlm.homework.entity.WrongGroup> getWrongTitlePapers(Long studentId) {
        try {
            log.info("根据学生ID获取错题组：studentId={}", studentId);
            // 根据学生ID查询错题组（截至当前时间）
            return wrongGroupRepository.findByStudentIdAndCreateTimeBefore(studentId, new Date());
        } catch (Exception e) {
            log.error("根据学生ID获取错题组失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
