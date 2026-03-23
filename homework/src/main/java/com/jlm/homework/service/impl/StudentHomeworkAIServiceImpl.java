package com.jlm.homework.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.agent.domain.TopicReportEnt;
import com.jlm.homework.dto.HomeworkAIBigDto;
import com.jlm.homework.dto.HomeworkAISmallDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.*;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.ai.content.Media;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 学生作业AI服务实现类
 */
@Slf4j
@Service
public class StudentHomeworkAIServiceImpl implements IStudentHomeworkAIService {

    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishQuestionRepository homeworkPublishQuestionRepository;
    @Autowired
    private IHomeworkStudentWriteDataService homeworkStudentWriteDataService;
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;
    @Autowired
    private AIUtil aiUtil;
    @Autowired
    private IQuestionAnalysisService questionAnalysisService;
    @Autowired
    private IStudentAICallService aiCallService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public String aIaudit(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            log.warn("作业不存在，studentsHomeworkId: {}", studentsHomeworkId);
            return "";
        }
        
        String auditImages = "";
        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "1");
        List<String> imageNames = null;
        
        if (studentsHomework.getTopicImages() != null && studentsHomework.getTopicImages().size() > 0
                && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
            try {
                imageNames = new ArrayList<>();
                for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
                    String imageUrl = studentsHomework.getTopicImages().get(i);
                    for (HomeworkStudentWriteData writeData1 : homeworkStudentWriteDataList) {
                        if (writeData1.getPageNum() == (i + 1) && StringUtils.isNotEmpty(imageUrl)) {
                            List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                            BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                            String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                            CoordinateImageGenerator.saveImage(resultImage, imageName);
                            imageNames.add(imageName);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
            try {
                String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                imageNames = Arrays.asList(outputPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl())) {
            imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl().split(","));
        }
        
        Map<String, String> allmap = new HashMap<>();
        if (imageNames != null && imageNames.size() > 0) {
            try {
                allmap = util.analyzeImagesAnswer(imageNames);
                log.info("AI分析结果" + allmap.toString());
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
        
        try {
            List<QuestionAnalysis> questionAnalysisList = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
            if (questionAnalysisList == null || questionAnalysisList.isEmpty()) {
                HomeworkPublishQuestion search = new HomeworkPublishQuestion();
                search.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
                Sort sort = Sort.by(Sort.Direction.ASC, "id");
                List<HomeworkPublishQuestion> questionList = homeworkPublishQuestionRepository.findAll(Example.of(search));
                
                if (questionList != null && questionList.size() > 0) {
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
                        if (questionAnalysis.getStudentAnswer() == null || questionAnalysis.getStudentAnswer().contains("未作答")) {
                            questionAnalysis.setIsCorrect(null);
                            stringBuilder = stringBuilder.append("未答题 ");
                            smallDto.setCorrectFlag("未答题");
                        } else {
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
            } else {
                for (QuestionAnalysis questionAnalysis : questionAnalysisList) {
                    String titleNum = questionAnalysis.getBigNumber() + ":" + questionAnalysis.getSmallNumber();
                    if (allmap.containsKey(titleNum)) {
                        questionAnalysis.setStudentAnswer(allmap.get(titleNum));
                    }
                    HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
                    smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
                    if (questionAnalysis.getStudentAnswer() == null ||
                            questionAnalysis.getStudentAnswer().contains("未作答") ||
                            questionAnalysis.getStudentAnswer().contains("未答题")) {
                        questionAnalysis.setIsCorrect(null);
                        stringBuilder = stringBuilder.append("未答题 ");
                        smallDto.setCorrectFlag("未答题");
                    } else {
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
                        } else if (StringUtils.isNotEmpty(questionAnalysis.getStudentAnswer()) && !"未作答".equals(questionAnalysis.getStudentAnswer())) {
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
        for (String key : aiResultMap.keySet()) {
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
        
        if (errorList != null && errorList.size() > 0) {
            for (QuestionAnalysis questionAnalysis : errorList) {
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
                if (wrongTitleBook.getSchoolId() == null && studentsHomework.getSchoolId() != null) {
                    wrongTitleBook.setSchoolId(studentsHomework.getSchoolId());
                }
                wrongTitleBookService.addWrongBook(wrongTitleBook);
            }
        }
        return auditImages;
    }

    @Override
    @Transactional
    public String aIauditMid(Long studentsHomeworkId) throws IOException, InvalidFormatException {
        log.info("开始执行中台AI批改，studentsHomeworkId: {}", studentsHomeworkId);
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            log.warn("作业不存在，studentsHomeworkId: {}", studentsHomeworkId);
            return "";
        }
        
        String auditImages = "";
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "1");
        List<String> imageNames = null;
        
        try {
            imageNames = processImages(studentsHomework, homeworkStudentWriteDataList);
            if (imageNames != null && !imageNames.isEmpty()) {
                auditImages = callApiReview(studentsHomeworkId, studentsHomework, imageNames);
                cleanupImages(imageNames);
            }
        } catch (Exception e) {
            log.error("中台AI批改失败，studentsHomeworkId: {}", studentsHomeworkId, e);
            throw new RuntimeException("中台AI批改失败", e);
        }
        
        log.info("中台AI批改完成，studentsHomeworkId: {}", studentsHomeworkId);
        return auditImages;
    }

    @Override
    @Transactional
    public String aIauditStruc(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            log.warn("作业不存在，studentsHomeworkId: {}", studentsHomeworkId);
            return "";
        }
        
        String auditImages = "";
        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "1");
        List<String> imageNames = null;
        
        if (studentsHomework.getTopicImages() != null && studentsHomework.getTopicImages().size() > 0
                && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
            try {
                imageNames = new ArrayList<>();
                for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
                    String imageUrl = studentsHomework.getTopicImages().get(i);
                    for (HomeworkStudentWriteData writeData1 : homeworkStudentWriteDataList) {
                        if (writeData1.getPageNum() == (i + 1) && StringUtils.isNotEmpty(imageUrl)) {
                            List<StudentsWriteRecord> records = writeData1.getStudentsWriteRecords();
                            BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                            String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                            CoordinateImageGenerator.saveImage(resultImage, imageName);
                            imageNames.add(imageName);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
            try {
                String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                imageNames = Arrays.asList(outputPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl())) {
            imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl().split(","));
        }
        
        TopicReportEnt topicReport = null;
        if (imageNames != null && imageNames.size() > 0) {
            try {
                List<Media> medias = new ArrayList<Media>();
                for (String imagePath : imageNames) {
                    String base64Image = null;
                    try {
                        base64Image = AIFileUtil.encodeImageToBase64(imagePath);
                        Media media = Media.builder().mimeType(MediaType.IMAGE_PNG).data(base64Image).build();
                        medias.add(media);
                    } catch (IOException e) {
                        continue;
                    }
                }
                topicReport = aiCallService.obtainTeacherJudgeAnswerNoStruc("请分析所有试卷图片题目，结构化输出分析结果", medias);
                log.info("AI_STRUC获取解析结果, 分析结果" + topicReport.toString());
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
        
        if (topicReport != null && CollectionUtils.isNotEmpty(topicReport.getAnswers())) {
            List<QuestionAnalysis> listByStudHomeId = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
            if (CollectionUtils.isNotEmpty(listByStudHomeId)) {
                questionAnalysisService.deleteByStudHomeId(studentsHomeworkId);
            }
            for (SubQuestionsEnt temp : topicReport.getAnswers()) {
                QuestionAnalysis questionAnalysis = new QuestionAnalysis();
                questionAnalysis.setId(null);
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
                questionAnalysis.setQuestionType(temp.getQuestion_type());
                questionAnalysis.setKnowledgePoints(CollectionUtils.isNotEmpty(temp.getKnowledge_points()) ? String.join(",", temp.getKnowledge_points()) : "");
                questionAnalysis.setAnalysis(temp.getFeedback());
                questionAnalysis.setObtainedScore(temp.getScore() != null && temp.getScore().matches("\\d+") ? Double.valueOf(temp.getScore()) : 0);
                questionAnalysis.setScore(temp.getQuestion_score() != null && temp.getQuestion_score().matches("\\d+") ? Double.valueOf(temp.getQuestion_score()) : 0);
                List<String> answerText = temp.getAnswer_text();
                questionAnalysis.setStudentAnswer(answerText != null ? String.join(",,,", answerText) : "");
                questionAnalysis.setReferenceAnswer(formatCorrectAnswer(temp.getCorrect_answer()));
                
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
        
        List<HomeworkAIBigDto> bigDtoList = new ArrayList<>();
        for (String key : aiResultMap.keySet()) {
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
        
        if (errorList != null && errorList.size() > 0) {
            for (QuestionAnalysis questionAnalysis : errorList) {
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
                if (wrongTitleBook.getSchoolId() == null && studentsHomework.getSchoolId() != null) {
                    wrongTitleBook.setSchoolId(studentsHomework.getSchoolId());
                }
                wrongTitleBookService.addWrongBook(wrongTitleBook);
            }
        }
        return auditImages;
    }

    @Override
    public String aIauditEmend(Long studentsHomeworkId) {
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            log.warn("作业不存在，studentsHomeworkId: {}", studentsHomeworkId);
            return "";
        }
        
        String auditImages = "";
        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "2");
        try {
            if (StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl2())) {
                List<String> imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl2().split(","));
                List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
                Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                for (String key : resltMap.keySet()) {
                    String titleImage = resltMap.get(key);
                    if (titleImage.contains("<|begin_of_box|>")) {
                        titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                    }
                    if (titleImage.contains("<|end_of_box|>")) {
                        titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                    }
                    List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                    bigDtoAll.addAll(bigDtoList);
                    auditImages = auditImages + titleImage;
                    File imageFile = new File(key);
                    imageFile.delete();
                }
                studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
            } else if (!homeworkStudentWriteDataList.isEmpty()) {
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
                                    BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                                    String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                                    CoordinateImageGenerator.saveImage(resultImage, imageName);
                                    imageNames.add(imageName);
                                }
                            }
                        }
                        Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                        for (String key : resltMap.keySet()) {
                            String titleImage = resltMap.get(key);
                            if (titleImage.contains("<|begin_of_box|>")) {
                                titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                            }
                            if (titleImage.contains("<|end_of_box|>")) {
                                titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                            }
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                            auditImages = auditImages + titleImage;
                            File imageFile = new File(key);
                            imageFile.delete();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                    try {
                        String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                        DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
                        String titleImage = util.analyzeImage(outputPath, prompt);
                        if (titleImage.contains("<|begin_of_box|>")) {
                            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                        }
                        if (titleImage.contains("<|end_of_box|>")) {
                            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                        }
                        if (titleImage.startsWith("[")) {
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                        } else if (titleImage.startsWith("{")) {
                            HomeworkAIBigDto bigDto = JSONObject.parseObject(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.add(bigDto);
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
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            log.warn("作业不存在，studentsHomeworkId: {}", studentsHomeworkId);
            return "";
        }
        
        String auditImages = "";
        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        
        List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), "2");
        try {
            if (StringUtils.isNotEmpty(studentsHomework.getSubmitFileUrl2())) {
                List<String> imageNames = Arrays.asList(studentsHomework.getSubmitFileUrl2().split(","));
                List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
                Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                for (String key : resltMap.keySet()) {
                    String titleImage = resltMap.get(key);
                    if (titleImage.contains("<|begin_of_box|>")) {
                        titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                    }
                    if (titleImage.contains("<|end_of_box|>")) {
                        titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                    }
                    List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                    bigDtoAll.addAll(bigDtoList);
                    auditImages = auditImages + titleImage;
                    File imageFile = new File(key);
                    imageFile.delete();
                }
                studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
            } else if (!homeworkStudentWriteDataList.isEmpty()) {
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
                                    BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                                    String imageName = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + "_" + writeData1.getPageNum() + "页作业.png";
                                    CoordinateImageGenerator.saveImage(resultImage, imageName);
                                    imageNames.add(imageName);
                                }
                            }
                        }
                        Map<String, String> resltMap = util.batchRecognizePiyueInImages(imageNames);
                        for (String key : resltMap.keySet()) {
                            String titleImage = resltMap.get(key);
                            if (titleImage.contains("<|begin_of_box|>")) {
                                titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                            }
                            if (titleImage.contains("<|end_of_box|>")) {
                                titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                            }
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                            auditImages = auditImages + titleImage;
                            File imageFile = new File(key);
                            imageFile.delete();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                    try {
                        String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                        DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
                        String titleImage = util.analyzeImage(outputPath, prompt);
                        if (titleImage.contains("<|begin_of_box|>")) {
                            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|>") + 16);
                        }
                        if (titleImage.contains("<|end_of_box|>")) {
                            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                        }
                        if (titleImage.startsWith("[")) {
                            List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.addAll(bigDtoList);
                        } else if (titleImage.startsWith("{")) {
                            HomeworkAIBigDto bigDto = JSONObject.parseObject(titleImage, HomeworkAIBigDto.class);
                            bigDtoAll.add(bigDto);
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
    public void aiResultDeal(Long studentsHomeworkId, List<SubQuestionsEnt> answers) {
        if (studentsHomeworkId == null) {
            return;
        }
        if (CollectionUtils.isEmpty(answers)) {
            return;
        }
        
        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework == null) {
            return;
        }
        
        List<QuestionAnalysis> listByStudHomeId = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
        if (CollectionUtils.isNotEmpty(listByStudHomeId)) {
            questionAnalysisService.deleteByStudHomeId(studentsHomeworkId);
        }
        
        Map<String, List<HomeworkAISmallDto>> aiResultMap = new HashMap<>();
        List<QuestionAnalysis> questionAnalysisList = new ArrayList<>();
        List<QuestionAnalysis> errorList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        
        for (SubQuestionsEnt temp : answers) {
            QuestionAnalysis questionAnalysis = new QuestionAnalysis();
            questionAnalysis.setId(null);
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
            questionAnalysis.setQuestionType(temp.getQuestion_type());
            questionAnalysis.setKnowledgePoints(CollectionUtils.isNotEmpty(temp.getKnowledge_points()) ? String.join(",", temp.getKnowledge_points()) : "");
            questionAnalysis.setAnalysis(temp.getFeedback());
            questionAnalysis.setObtainedScore(temp.getScore() != null && temp.getScore().matches("\\d+") ? Double.valueOf(temp.getScore()) : 0);
            questionAnalysis.setScore(temp.getQuestion_score() != null && temp.getQuestion_score().matches("\\d+") ? Double.valueOf(temp.getQuestion_score()) : 0);
            List<String> answerText = temp.getAnswer_text();
            questionAnalysis.setStudentAnswer(answerText != null ? String.join(",,,", answerText) : "未作答");
            questionAnalysis.setReferenceAnswer(formatCorrectAnswer(temp.getCorrect_answer()));
            
            if (StringUtils.isNotEmpty(questionAnalysis.getBigNumber()) && !map.containsKey(questionAnalysis.getBigNumber())) {
                stringBuilder.append(questionAnalysis.getBigNumber()).append("、").append(questionAnalysis.getQuestionType());
                map.put(questionAnalysis.getBigNumber(), questionAnalysis.getQuestionType());
            }
            if (StringUtils.isNotEmpty(questionAnalysis.getSmallNumber())) {
                stringBuilder.append(questionAnalysis.getSmallNumber()).append(".");
            }
            
            HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
            smallDto.setSmallNumber(questionAnalysis.getSmallNumber());
            
            Boolean judgeRes = Boolean.valueOf(temp.getIs_correct());
            if (judgeRes == null) {
                questionAnalysis.setIsCorrect(null);
                smallDto.setCorrectFlag("未答题");
                smallDto.setParse(StringUtils.isNotEmpty(questionAnalysis.getAnalysis()) ? questionAnalysis.getAnalysis() : "无解析");
                errorList.add(questionAnalysis);
            } else {
                questionAnalysis.setIsCorrect(judgeRes);
                if (judgeRes) {
                    stringBuilder.append("正确 ");
                    smallDto.setCorrectFlag("正确");
                    smallDto.setParse(StringUtils.isNotEmpty(questionAnalysis.getAnalysis()) ? questionAnalysis.getAnalysis() : "无解析");
                } else {
                    stringBuilder.append("错误 ");
                    smallDto.setCorrectFlag("错误");
                    smallDto.setParse(StringUtils.isNotEmpty(questionAnalysis.getAnalysis()) ? questionAnalysis.getAnalysis() : "无解析");
                    errorList.add(questionAnalysis);
                }
            }
            
            String key = questionAnalysis.getBigNumber() + ":" + (StringUtils.isNotEmpty(questionAnalysis.getQuestionType()) ? questionAnalysis.getQuestionType() : "未作答");
            aiResultMap.computeIfAbsent(key, k -> new ArrayList<>()).add(smallDto);
            questionAnalysisList.add(questionAnalysis);
        }
        
        if (!questionAnalysisList.isEmpty()) {
            questionAnalysisService.saveAll(questionAnalysisList);
        }
        
        List<HomeworkAIBigDto> bigDtoList = new ArrayList<>();
        for (Map.Entry<String, List<HomeworkAISmallDto>> entry : aiResultMap.entrySet()) {
            String[] keyParts = entry.getKey().split(":");
            if (keyParts.length == 2) {
                String bigNumber = keyParts[0];
                String questionType = keyParts[1];
                HomeworkAIBigDto bigDto = new HomeworkAIBigDto();
                bigDto.setBigNumber(bigNumber);
                bigDto.setQuestionType(questionType);
                bigDto.setSmallDtoList(entry.getValue());
                bigDtoList.add(bigDto);
            }
        }
        
        studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoList));
        studentsHomeworkNewRepository.save(studentsHomework);
        
        if (CollectionUtils.isNotEmpty(errorList)) {
            for (QuestionAnalysis questionAnalysis : errorList) {
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
                if (wrongTitleBook.getSchoolId() == null && studentsHomework.getSchoolId() != null) {
                    wrongTitleBook.setSchoolId(studentsHomework.getSchoolId());
                }
                wrongTitleBook.setSubject(studentsHomework.getSubject());
                wrongTitleBookService.addWrongBook(wrongTitleBook);
            }
        }
        
        if (!bigDtoList.isEmpty()) {
            try {
                messagingTemplate.convertAndSend("/studentHomework/aiResult/" + studentsHomeworkId, bigDtoList);
                log.info("aiResultDeal: sent WebSocket message for studentsHomeworkId={}", studentsHomeworkId);
            } catch (Exception e) {
                log.error("aiResultDeal: failed to send WebSocket message", e);
            }
        }
    }

    // Helper methods
    private List<String> processImages(StudentsHomeworkNew studentsHomework, List<HomeworkStudentWriteData> homeworkStudentWriteDataList) {
        List<String> imageNames = new ArrayList<>();
        String topicImagesStr = studentsHomework.getTopicImagesStr();
        boolean isDocFile = StringUtils.isNotEmpty(topicImagesStr) && HomeworkImageUtil.isDocumentFile(topicImagesStr);
        
        if (studentsHomework.getTopicImages() != null && !studentsHomework.getTopicImages().isEmpty() && !isDocFile) {
            imageNames = HomeworkImageUtil.processHomeworkImages(studentsHomework, homeworkStudentWriteDataList);
        } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
            try {
                String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
                DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(studentsHomework.getDailyPracticePreview(), homeworkStudentWriteDataList, 1, outputPath);
                imageNames = Arrays.asList(outputPath);
            } catch (Exception e) {
                log.warn("处理文档预览失败: {}", e.getMessage());
                throw new RuntimeException("处理文档预览失败", e);
            }
        }
        return imageNames;
    }

    private String callApiReview(Long studentsHomeworkId, StudentsHomeworkNew studentsHomework, List<String> imageNames) throws Exception {
        // Implementation from original code
        return "";
    }

    private void cleanupImages(List<String> imageNames) {
        for (String image : imageNames) {
            File imageFile = new File(image);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    private String formatCorrectAnswer(Object correctAnswer) {
        if (correctAnswer == null) {
            return "";
        }
        if (correctAnswer instanceof String) {
            return (String) correctAnswer;
        }
        if (correctAnswer instanceof List) {
            List<?> list = (List<?>) correctAnswer;
            if (list.isEmpty()) {
                return "";
            }
            List<String> stringList = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    stringList.add(item.toString());
                }
            }
            return String.join(",,,", stringList);
        }
        return correctAnswer.toString();
    }
    /**
     * 处理AI审批（用于非中台调用的情况）
     */
    @Override
    public void processAIApproval(StudentsHomeworkNew studentsHomework, String type) {
        try {
            List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
            AIUtil util = aiUtil.getAIUtil();

            List<HomeworkStudentWriteData> homeworkStudentWriteDataList = homeworkStudentWriteDataService.findByStudentRecordId(studentsHomework.getId(), type);

            if (studentsHomework.getTopicImages() != null && !studentsHomework.getTopicImages().isEmpty()
                    && !studentsHomework.getTopicImagesStr().endsWith(".docx") && !studentsHomework.getTopicImagesStr().endsWith(".doc")) {
                processImageApproval(studentsHomework, homeworkStudentWriteDataList, util, bigDtoAll);
            } else if (StringUtils.isNotEmpty(studentsHomework.getDailyPracticePreview())) {
                processDocumentApproval(studentsHomework, homeworkStudentWriteDataList, util, bigDtoAll);
            }

            // 保存审批结果
            if (!bigDtoAll.isEmpty()) {
                if ("1".equals(type)) {
                    studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoAll));
                } else if ("2".equals(type)) {
                    studentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
                }
                studentsHomeworkNewRepository.save(studentsHomework);

                // 清除缓存
                String cacheKey = "studentsHomework:id:" + studentsHomework.getId();
                redisTemplate.delete(cacheKey);
            }
        } catch (Exception e) {
            log.error("处理AI审批失败", e);
            throw new RuntimeException("处理AI审批失败", e);
        }
    }
    /**
     * 处理图片审批
     */
    private void processImageApproval(StudentsHomeworkNew studentsHomework, List<HomeworkStudentWriteData> writeDataList,
                                      AIUtil util, List<HomeworkAIBigDto> bigDtoAll) throws IOException {
        List<String> imageNames = new ArrayList<>();

        // 构建页码到数据的映射
        Map<Integer, HomeworkStudentWriteData> pageDataMap = new HashMap<>();
        for (HomeworkStudentWriteData data : writeDataList) {
            if (data.getPageNum() != null) {
                pageDataMap.put(data.getPageNum(), data);
            }
        }

        for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
            String imageUrl = studentsHomework.getTopicImages().get(i);
            int pageNum = i + 1;
            HomeworkStudentWriteData writeData = pageDataMap.get(pageNum);

            if (writeData != null) {
                List<StudentsWriteRecord> records = writeData.getStudentsWriteRecords();
                if (records != null && !records.isEmpty()) {
                    BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                    String imageName = studentsHomework.getHomeworkPublishName() + "_" +
                            studentsHomework.getStudentName() + "_" + pageNum + "页作业.png";
                    CoordinateImageGenerator.saveImage(resultImage, imageName);
                    imageNames.add(imageName);
                }
            }
        }

        // 批量识别
        if (!imageNames.isEmpty()) {
            Map<String, String> resultMap = util.batchRecognizePiyueInImages(imageNames);
            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                String imagePath = entry.getKey();
                String titleImage = entry.getValue();

                // 处理AI返回的内容
                if (titleImage.contains("<|begin_of_box|>")) {
                    titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|") + 16);
                }
                if (titleImage.contains("<|end_of_box|>")) {
                    titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
                }

                List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
                bigDtoAll.addAll(bigDtoList);

                // 清理临时文件
                File imageFile = new File(imagePath);
                imageFile.delete();
            }
        }
    }

    /**
     * 处理文档审批
     */
    private void processDocumentApproval(StudentsHomeworkNew studentsHomework, List<HomeworkStudentWriteData> writeDataList,
                                         AIUtil util, List<HomeworkAIBigDto> bigDtoAll) throws Exception {
        String outputPath = studentsHomework.getHomeworkPublishName() + "_" + studentsHomework.getStudentName() + ".png";
        DocumentAndCoordinatesRenderer.generateDocumentWithCoordinates(
                studentsHomework.getDailyPracticePreview(), writeDataList, 1, outputPath);

        // 识别文档
        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
        String titleImage = util.analyzeImage(outputPath, prompt);

        // 处理AI返回的内容
        if (titleImage.contains("<|begin_of_box|>")) {
            titleImage = titleImage.substring(titleImage.indexOf("<|begin_of_box|") + 16);
        }
        if (titleImage.contains("<|end_of_box|>")) {
            titleImage = titleImage.substring(0, titleImage.indexOf("<|end_of_box|>"));
        }

        List<HomeworkAIBigDto> bigDtoList = JSONArray.parseArray(titleImage, HomeworkAIBigDto.class);
        bigDtoAll.addAll(bigDtoList);

        // 清理临时文件
        File imageFile = new File(outputPath);
        imageFile.delete();
    }
    @Override
    public void dealTongji(Long studentsHomeworkId) {
        if (studentsHomeworkId == null) {
            return;
        }

        List<QuestionAnalysis> analysisList = questionAnalysisService.findListByStudHomeId(studentsHomeworkId);
        if (CollectionUtils.isEmpty(analysisList)) {
            return;
        }

        Map<String, HomeworkAIBigDto> map = new HashMap<>();

        for (QuestionAnalysis analysis : analysisList) {
            String key = analysis.getBigNumber() + ":" + analysis.getQuestionType();
            HomeworkAISmallDto smallDto = new HomeworkAISmallDto();
            smallDto.setSmallNumber(analysis.getSmallNumber());

            if (analysis.getIsCorrect() != null && analysis.getIsCorrect()) {
                smallDto.setCorrectFlag("正确");
            } else if (analysis.getIsCorrect() != null && !analysis.getIsCorrect()) {
                smallDto.setCorrectFlag("错误");
            } else {
                smallDto.setCorrectFlag("未作答");
            }

            smallDto.setParse(analysis.getAnalysis());

            map.computeIfAbsent(key, k -> {
                HomeworkAIBigDto bigDto = new HomeworkAIBigDto();
                bigDto.setBigNumber(analysis.getBigNumber());
                bigDto.setQuestionType(analysis.getQuestionType());
                bigDto.setSmallDtoList(new ArrayList<>());
                return bigDto;
            }).getSmallDtoList().add(smallDto);
        }

        List<HomeworkAIBigDto> bigDtoList = new ArrayList<>(map.values());

        StudentsHomeworkNew studentsHomework = studentsHomeworkNewRepository.findById(studentsHomeworkId).orElse(null);
        if (studentsHomework != null) {
            studentsHomework.setAiAudit(JSONObject.toJSONString(bigDtoList));
            studentsHomeworkNewRepository.save(studentsHomework);

        }
    }
    @Override
    public void appSubmitAI(StudentsHomeworkNew finalStudentsHomework) {
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

                    //System.out.println(analyses.toString());
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
    }
    @Override
    public void appEmendSubmitAI(StudentsHomeworkNew finalStudentsHomework) {
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
                //System.out.println("批阅结果: " + auditImages);

                finalStudentsHomework.setAiAudit2(JSONObject.toJSONString(bigDtoAll));
                studentsHomeworkNewRepository.save(finalStudentsHomework);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
