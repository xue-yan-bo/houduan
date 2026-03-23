package com.jlm.homework.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.jlm.agent.AIServ.ZhiPuAIAgent;
import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.agent.domain.TopicReportEnt;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.HomeworkAIBigDto;
import com.jlm.homework.dto.HomeworkPublishRequest;
import com.jlm.homework.dto.StudentsHomeworkSimpleDTO;
import com.jlm.homework.entity.*;
import com.jlm.homework.repository.HomeworkPublishQuestionRepository;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.*;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.mapping.Join;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import static com.jlm.homework.util.AIFileUtil.encodeImageToBase64;
@Slf4j
@Service
public class HomeworkPublishServiceImpl implements IHomeworkPublishService {
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
    @Autowired
    private IUserService userService;
    @Autowired
    private ZhipuAIConfig zhipuAIConfig;
    @Autowired
    private AIUtil aiUtil;
    @Autowired
    private HomeworkPublishQuestionRepository homeworkPublishQuestionRepository;
    @Autowired
    private IStudentAICallService studentAICallService;
    @Autowired
    private WordToPdfUtil wordToPdfUtil;

    @Override
    public String create(HomeworkPublish homeworkPublish) {
        if (homeworkPublish != null
                && homeworkPublish.getScheduledReleaseFlag() == 0
                && homeworkPublish.getPublishTime() == null) {
            homeworkPublish.setPublishTime(new Date());
        }
        homeworkPublish.setDeleteFlag(0);
        if(homeworkPublish.getSchoolId()==null){
            homeworkPublish.setSchoolId(userService.getCurrentSchoolId());
        }
        if (homeworkPublish.getClassId() != null && homeworkPublish.getClassId().size() > 0) {
            String classIds = homeworkPublish.getClassId().stream().map(Object::toString).collect(Collectors.joining(","));
            homeworkPublish.setClassIds(classIds);
        }
        if (homeworkPublish.getClassName() != null && homeworkPublish.getClassName().size() > 0) {
            homeworkPublish.setClassNames(String.join(",", homeworkPublish.getClassName()));
        }
        if (homeworkPublish.getTopicImages() != null && homeworkPublish.getTopicImages().size() > 0) {
            homeworkPublish.setTopicImagesStr(String.join(" ,", homeworkPublish.getTopicImages()));
        }
        if (homeworkPublish.getScheduledReleaseFlag() == 0) {
            homeworkPublish.setPublishStatus(1);
        } else {
            homeworkPublish.setPublishStatus(0);
        }

        if (StringUtils.isEmpty(homeworkPublish.getSubject())) {
            String subject = null;
            if (homeworkPublish.getExerciseBookId() != null) {
                ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                subject = exerciseBook.getSubject();
            }
            homeworkPublish.setSubject(subject);
        }
        if(Integer.compare(1,homeworkPublish.getTestSource())==0&&!homeworkPublish.getTopicImages().isEmpty()){
            List<String> urls = new ArrayList<>();
            for(String url:homeworkPublish.getTopicImages()){
                if(StringUtils.isNotEmpty(url)&&
                        (url.contains(".docx")||url.contains(".doc"))) {
                    try {
                        String pdfurl = wordToPdfUtil.convertMinioWordToPdf(url);
                        urls.add(pdfurl);
                    } catch (Exception e) {
                        log.error("转换PDF错误："+e.getMessage());
                        urls.add(url);
                    }
                }else{
                    urls.add(url);
                }
            }
            homeworkPublish.setTopicImages(urls);
            homeworkPublish.setTopicImagesStr(String.join(",",urls));
        }
        if (StringUtils.isEmpty(homeworkPublish.getUserId()) && userService.getCurrentUserInfo() != null) {
            CurrentUserInfo userInfo = userService.getCurrentUserInfo();
            if (StringUtils.isNotEmpty(userInfo.getUserUuid())) {
                homeworkPublish.setUserId(userInfo.getUserUuid());
            }
        }
        homeworkPublishRepository.save(homeworkPublish);

        if (homeworkPublish.getScheduledReleaseFlag() == 0) {//如果不是定时发布，就是立刻发布，生成学生作业
            studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
        } else if (homeworkPublish.getScheduledReleaseFlag() == 1
                && homeworkPublish.getPublishTime() != null) {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
                    homeworkPublish.setPublishStatus(1);
                    homeworkPublish.setAuditStatus(1);
                    homeworkPublishRepository.updatePublishStatus(homeworkPublish.getId(),1);
                }
            };
            timer.schedule(task, homeworkPublish.getPublishTime());
        }
        if (homeworkPublish.getDeadline() != null) {
            Timer timer = new Timer();
            TimerTask task1 = new TimerTask() {
                @Override
                public void run() {
                    studentsHomeworkNewService.endStudentsHomework(homeworkPublish);
                    homeworkPublish.setPublishStatus(2);
                    homeworkPublishRepository.updatePublishStatus(homeworkPublish.getId(),2);
                }
            };
            timer.schedule(task1, homeworkPublish.getDeadline());
        }

        FutureTask<String> futureTask = new FutureTask<>(() -> {

            this.homeworkQuestionStuc(homeworkPublish);


            return "异步-OK";


        });
        Thread thread = new Thread(futureTask);
        thread.start();
        return homeworkPublish.getId().toString();
    }

    @Override
    public HomeworkPublish getById(Long id) {
        Optional<HomeworkPublish> optional = homeworkPublishRepository.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Override
    public HomeworkPublish update(HomeworkPublish homeworkPublish) {
        homeworkPublish.setDeleteFlag(0);
        homeworkPublish.setPublishStatus(0);
        if (homeworkPublish.getClassId() != null && homeworkPublish.getClassId().size() > 0) {
            String classIds = homeworkPublish.getClassId().stream().map(Object::toString).collect(Collectors.joining(","));
            homeworkPublish.setClassIds(classIds);
        }
        if (homeworkPublish.getClassName() != null && homeworkPublish.getClassName().size() > 0) {
            homeworkPublish.setClassNames(String.join(",", homeworkPublish.getClassName()));
        }
        if (homeworkPublish.getTopicImages() != null && homeworkPublish.getTopicImages().size() > 0) {
            homeworkPublish.setTopicImagesStr(String.join(" ,", homeworkPublish.getTopicImages()));
        }
        if (1 == homeworkPublish.getTestSource()) {//练习册，清空每日一练数据
            homeworkPublish.setDailyPracticeld(null);
            homeworkPublish.setDailyPracticeName(null);
            homeworkPublish.setDailyPracticePreview(null);
        } else if (3 == homeworkPublish.getTestSource()) {//每日一练，练习册数据
            homeworkPublish.setExerciseBookId(null);
            homeworkPublish.setExerciseBookName(null);
        }
        if (StringUtils.isEmpty(homeworkPublish.getSubject())) {
            String subject = null;
            if (homeworkPublish.getExerciseBookId() != null) {
                ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                subject = exerciseBook.getSubject();
            }
            homeworkPublish.setSubject(subject);
        }
        if(Integer.compare(1,homeworkPublish.getTestSource())==0&&!homeworkPublish.getTopicImages().isEmpty()){
            List<String> urls = new ArrayList<>();
            for(String url:homeworkPublish.getTopicImages()){
                if(StringUtils.isNotEmpty(url)
                        &&(url.contains(".docx")||url.contains(".doc"))) {
                    try {
                        String pdfurl = wordToPdfUtil.convertMinioWordToPdf(url);
                        urls.add(pdfurl);
                    } catch (Exception e) {
                        log.error("转换PDF错误："+e.getMessage());
                        urls.add(url);
                    }
                }else{
                    urls.add(url);
                }
            }
            homeworkPublish.setTopicImages(urls);
            homeworkPublish.setTopicImagesStr(String.join(",",urls));
        }
        return homeworkPublishRepository.save(homeworkPublish);
    }

    @Override
    public Page<HomeworkPublish> selectList(Integer pageNum, Integer pageSize, HomeworkPublishRequest homeworkPublishRequest) {
        if (pageNum == null || pageNum <= 0 || pageSize == null || pageSize <= 0) {
            pageNum = 1;
            pageSize = 10;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition = null;
                if (StringUtils.isNotEmpty(homeworkPublishRequest.getHomeworkName())) {
                    condition = criteriaBuilder.like(root.get("homeworkName"), "%" + homeworkPublishRequest.getHomeworkName() + "%");
                } else {
                    condition = criteriaBuilder.conjunction();
                }
                Predicate cond1 = null;
                if (StringUtils.isNotEmpty(homeworkPublishRequest.getClassIds())) {
                    cond1 = criteriaBuilder.like(root.get("classIds"), "%" + homeworkPublishRequest.getClassIds() + "%");
                } else {
                    cond1 = criteriaBuilder.conjunction();
                }
                Predicate cond2 = null;
                Predicate cond3 = null;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();
                try {
                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if (StringUtils.isNotEmpty(homeworkPublishRequest.getPublishTime())) {

                        Date publishTime = sdf.parse(homeworkPublishRequest.getPublishTime());

                        Date publishTime1 = sdf1.parse(homeworkPublishRequest.getPublishTime() + " 23:59:59");
                        cond2 = criteriaBuilder.between(root.get("publishTime"), publishTime, publishTime1);
                    } else {
                        cond2 = criteriaBuilder.conjunction();
                    }
                    if (StringUtils.isNotEmpty(homeworkPublishRequest.getDeadline())) {

                        Date deadline = sdf.parse(homeworkPublishRequest.getDeadline());
                        Date deadline1 = sdf1.parse(homeworkPublishRequest.getDeadline() + " 23:59:59");
                        cond3 = criteriaBuilder.between(root.get("deadline"), deadline, deadline1);
                    } else {
                        cond3 = criteriaBuilder.conjunction();
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                Predicate cond4 = null;
                if (homeworkPublishRequest.getTestSource() != null) {
                    cond4 = criteriaBuilder.equal(root.get("testSource"), homeworkPublishRequest.getTestSource());
                } else {
                    cond4 = criteriaBuilder.conjunction();
                }
                Predicate cond5 = null;
                if (homeworkPublishRequest.getAuditStatus() != null) {
                    cond5 = criteriaBuilder.equal(root.get("auditStatus"), homeworkPublishRequest.getAuditStatus());
                } else {
                    cond5 = criteriaBuilder.conjunction();
                }
                Predicate cond6 = criteriaBuilder.equal(root.get("deleteFlag"), 0);
                Predicate cond7 = null;
                CurrentUserInfo currentUserInfo = userService.getCurrentUserInfo();
                if (StringUtils.isNotEmpty(homeworkPublishRequest.getUserId())) {
                    cond7 = criteriaBuilder.equal(root.get("userId"), homeworkPublishRequest.getUserId());
                } else if (currentUserInfo != null && StringUtils.isNotEmpty(currentUserInfo.getUserUuid())) {
                    cond7 = criteriaBuilder.equal(root.get("userId"), currentUserInfo.getUserUuid());
                } else {
                    cond7 = criteriaBuilder.conjunction();
                }
                Predicate cond8 = null;
                Long schoolId = userService.getCurrentSchoolId();
                if (homeworkPublishRequest.getSchoolId() != null) {
                    cond8 = criteriaBuilder.equal(root.get("schoolId"), homeworkPublishRequest.getSchoolId());
                } else if (schoolId != null) {
                    cond8 = criteriaBuilder.equal(root.get("schoolId"), schoolId);
                } else {
                    cond8 = criteriaBuilder.conjunction();
                }
                query.where(condition, cond1, cond2, cond3, cond4, cond5, cond6, cond7, cond8);
                return null;
            }
        };
        Page<HomeworkPublish> page = homeworkPublishRepository.findAll(specification, pageable);
        for (HomeworkPublish publish : page.getContent()) {
            Long submitNum = studentsHomeworkNewService.getSubmitNumByHomeworkPublishId(publish.getId());
            publish.setSubmitNum(submitNum);
        }
        return page;
    }

    @Override
    public void deleteById(Long id) {
        HomeworkPublish homeworkPublish = homeworkPublishRepository.getReferenceById(id);
        homeworkPublish.setDeleteFlag(1);
        homeworkPublishRepository.save(homeworkPublish);
    }

    @Override
    public void withdraw(Long homeworkPublishId) {
        Optional<HomeworkPublish> optional = homeworkPublishRepository.findById(homeworkPublishId);
        if (optional.isPresent()) {
            HomeworkPublish homeworkPublish = optional.get();
            Specification<StudentsHomeworkNew> stuSpecification = new Specification<StudentsHomeworkNew>() {

                @Override
                public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    List<Predicate> list = new ArrayList<>();

                    Predicate cond = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                    list.add(cond);
                    Predicate cond1 = criteriaBuilder.isNotNull(root.get("startTime"));
                    list.add(cond1);
                    Predicate[] p = new Predicate[list.size()];
                    return criteriaBuilder.and(list.toArray(p));
                }
            };
            List<StudentsHomeworkSimpleDTO> studentList = studentsHomeworkNewService.findAllSimpleDTOBySpecification(stuSpecification);
            if (studentList != null && studentList.size() > 0) {
                throw new RuntimeException("已有学生提交该作业，不能撤回发布！");
            }
            studentsHomeworkNewService.updateSubmietNull(homeworkPublishId);
            homeworkPublish.setPublishStatus(0);
            homeworkPublishRepository.save(homeworkPublish);
            HomeworkPublishQuestion quSearch = new HomeworkPublishQuestion();
            quSearch.setHomeworkPublishId(homeworkPublishId);
            homeworkPublishQuestionRepository.delete(quSearch);
        }
    }

    @Override
    public void rePublish(Long homeworkPublishId) {
        Optional<HomeworkPublish> optional = homeworkPublishRepository.findById(homeworkPublishId);
        if (optional.isPresent()) {
            HomeworkPublish homeworkPublish = optional.get();
            if (homeworkPublish.getPublishStatus() > 0) {
                return;
            }
            List<StudentsHomeworkSimpleDTO> homeworkNewList = studentsHomeworkNewService.getByHomeworkPublishId(homeworkPublish.getId(), null);
            if (homeworkNewList.isEmpty()) {
                if (homeworkPublish.getScheduledReleaseFlag() == 0) {//如果不是定时发布，就是立刻发布，生成学生作业
                    studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
                } else if (homeworkPublish.getScheduledReleaseFlag() == 1
                        && homeworkPublish.getPublishTime() != null) {
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
                            homeworkPublish.setPublishStatus(1);
                            homeworkPublishRepository.updatePublishStatus(homeworkPublish.getId(),1);
                        }
                    };
                    timer.schedule(task, homeworkPublish.getPublishTime());
                }
            } else {
                studentsHomeworkNewService.updateByPublishId(homeworkPublishId,
                        homeworkPublish.getHomeworkName(),
                        homeworkPublish.getTopicImagesStr(),
                        homeworkPublish.getDeadline(),
                        homeworkPublish.getDailyPracticeld(),
                        homeworkPublish.getDailyPracticeName(),
                        homeworkPublish.getDailyPracticePreview(),
                        homeworkPublish.getChapter(),
                        homeworkPublish.getKnowledgePoint(), 0);
            }
            if (homeworkPublish.getDeadline() != null) {
                Timer timer = new Timer();
                TimerTask task1 = new TimerTask() {
                    @Override
                    public void run() {
                        studentsHomeworkNewService.endStudentsHomework(homeworkPublish);
                        homeworkPublish.setPublishStatus(2);
                        homeworkPublishRepository.updatePublishStatus(homeworkPublish.getId(),2);
                    }
                };
                timer.schedule(task1, homeworkPublish.getDeadline());
            }
            if (StringUtils.isEmpty(homeworkPublish.getSubject())) {
                String subject = null;
                if (homeworkPublish.getExerciseBookId() != null) {
                    ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                    subject = exerciseBook.getSubject();
                }
                homeworkPublish.setSubject(subject);
            }
            homeworkPublish.setPublishStatus(1);
            homeworkPublishRepository.save(homeworkPublish);
            FutureTask<String> futureTask = new FutureTask<>(() -> {

                this.homeworkQuestionStuc(homeworkPublish);

                return "异步-OK";

            });
            Thread thread = new Thread(futureTask);
            thread.start();
        }
    }

    public void aIHomeworkPublic(Long homeworkPublishId) {
        HomeworkPublish homeworkPublish = homeworkPublishRepository.findById(homeworkPublishId).orElse(null);
        if (homeworkPublish != null) {
            homeworkQuestion(homeworkPublish);
        }
    }

    // AI
    public void aIHomeworkPublicStruc(Long homeworkPublishId) {
        HomeworkPublish homeworkPublish = homeworkPublishRepository.findById(homeworkPublishId).orElse(null);
        if (homeworkPublish != null) {
            homeworkQuestionStuc(homeworkPublish);
        }
    }

    private void homeworkQuestion(HomeworkPublish homeworkPublish) {
        String auditImages = "";
        List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
        //异步处理AI智能审批
        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        //QianWenAIUtil util = new QianWenAIUtil();
        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }
        List<HomeworkPublishQuestion> questionList = new ArrayList<>();
        if (homeworkPublish.getTopicImages() != null && homeworkPublish.getTopicImages().size() > 0
                && !homeworkPublish.getTopicImagesStr().endsWith(".docx") && !homeworkPublish.getTopicImagesStr().endsWith(".doc")) {
            try {
                List<String> imageNames = homeworkPublish.getTopicImages();

                questionList = util.reviewHomreWorkQuestions(imageNames);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (StringUtils.isNotEmpty(homeworkPublish.getDailyPracticePreview())) {
            try {


                List<String> imageNames = DocumentAndCoordinatesRenderer.generateAllPagesDocumentWithCoordinates(homeworkPublish.getDailyPracticePreview(), null, homeworkPublish.getHomeworkName());

                //试题识别

                questionList = util.reviewHomreWorkQuestions(imageNames);
                imageNames.stream().forEach(imageName -> {
                    File file = new File(imageName);
                    file.delete();
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (questionList != null && questionList.size() > 0) {
            for (HomeworkPublishQuestion question : questionList) {
                question.setHomeworkPublishId(homeworkPublish.getId());
                homeworkPublishQuestionRepository.save(question);
            }
        }
    }


    private void homeworkQuestionStuc(HomeworkPublish homeworkPublish) {
        String auditImages = "";
        List<HomeworkAIBigDto> bigDtoAll = new ArrayList<>();
        //异步处理AI智能审批
        //ZhipuAIImageAnalysisUtil util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
        //QianWenAIUtil util = new QianWenAIUtil();

        AIUtil util = aiUtil.getAIUtil();
        if ("qianwen".equals(util.getAiName())) {
            util = (QianWenAIUtil) util;
        } else {
            util = (ZhipuAIImageAnalysisUtil) util;
        }


        List<HomeworkPublishQuestion> questionList = new ArrayList<>();
        if (homeworkPublish.getTopicImages() != null && homeworkPublish.getTopicImages().size() > 0
                && !homeworkPublish.getTopicImagesStr().endsWith(".docx") && !homeworkPublish.getTopicImagesStr().endsWith(".doc")) {
            try {
                List<String> imageNames = homeworkPublish.getTopicImages();

                List<Media> medias = new ArrayList<Media>();
                for (String imageUrl : imageNames) {
                    String base64Str = encodeImageToBase64(imageUrl);
                    Media media = Media.builder().mimeType(MediaType.IMAGE_JPEG).data(base64Str).build();
                    medias.add(media);
                }
                TopicReportEnt topicReportEnt = studentAICallService.obtainTeacherAnswerNoStruc("请仔细分析这份试卷，输出所有题目的题号，学生答案，解析答案", medias);
                questionList = parseAIResultToHomreWorkQuestionStucList(topicReportEnt);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (StringUtils.isNotEmpty(homeworkPublish.getDailyPracticePreview())) {
            try {

                List<String> imageNames = DocumentAndCoordinatesRenderer.generateAllPagesDocumentWithCoordinates(homeworkPublish.getDailyPracticePreview(), null, homeworkPublish.getHomeworkName());

                //试题识别

                questionList = util.reviewHomreWorkQuestions(imageNames);
                imageNames.stream().forEach(imageName -> {
                    File file = new File(imageName);
                    file.delete();
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (questionList != null && questionList.size() > 0) {
            for (HomeworkPublishQuestion question : questionList) {
                question.setHomeworkPublishId(homeworkPublish.getId());
                homeworkPublishQuestionRepository.save(question);
            }
        }

    }

    private List<HomeworkPublishQuestion> parseAIResultToHomreWorkQuestionStucList(TopicReportEnt report) {
        List<HomeworkPublishQuestion> questionAnalysisList = new ArrayList<>();


        List<SubQuestionsEnt> answers = report.getAnswers();

        try {
            // 当前正在处理的题目

            for (SubQuestionsEnt subQue : answers) {
                HomeworkPublishQuestion currentQuestion = new HomeworkPublishQuestion();

                String majorQuestionId = subQue.getMajor_question_id();  //大题号
                String questionId = subQue.getQuestion_id();
                String questionType = subQue.getQuestion_type();
                String content = subQue.getQuestion_content();
                List<String> knowledgePoints = subQue.getKnowledge_points();
                Object correctAnswerObj = subQue.getCorrect_answer();
                String correctAnswer = formatCorrectAnswer(correctAnswerObj);

                currentQuestion.setBigNumber(majorQuestionId); //大题号
                currentQuestion.setSmallNumber(questionId);  //小题号
                currentQuestion.setQuestionType(questionType); //问题类型
                currentQuestion.setReferenceAnswer(correctAnswer); //答案
                currentQuestion.setContent(content); //问题内容
                currentQuestion.setKnowledgePoints(CollectionUtils.isEmpty(knowledgePoints) ? null : String.join(",", knowledgePoints)); //参考知识点
                questionAnalysisList.add(currentQuestion);
            }
        } catch (Exception e) {
            // 如果解析失败，记录错误并返回空列表
            System.err.println("解析AI结果失败: " + e.getMessage());
        }
        return questionAnalysisList;
    }

    private String formatCorrectAnswer(Object correctAnswer) {
        if (correctAnswer == null) {
            return null;
        }
        
        if (correctAnswer instanceof String) {
            return (String) correctAnswer;
        }
        
        if (correctAnswer instanceof List) {
            List<?> list = (List<?>) correctAnswer;
            if (list.isEmpty()) {
                return null;
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


}
