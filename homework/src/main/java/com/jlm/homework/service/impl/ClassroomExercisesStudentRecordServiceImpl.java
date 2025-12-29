package com.jlm.homework.service.impl;

import com.jlm.agent.AIServ.ZhiPuAIAgent;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.ExerciseWriteData;
import com.jlm.homework.dto.StudentWriteDto;
import com.jlm.homework.dto.TeacherWriteDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IClassroomExercisesStudentRecordService;
import com.jlm.homework.service.IClassroomStudentWriteDataService;
import com.jlm.homework.service.IClassroomTeacherWriteDataService;
import com.jlm.homework.service.IStudentAICallService;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClassroomExercisesStudentRecordServiceImpl implements IClassroomExercisesStudentRecordService {
    @Resource
    private ClassroomExercisesRepository classroomExercisesRepository;
    @Resource
    private ClassroomExercisesStudentRecordRepository classroomExercisesStudentRecordRepository;
    @Autowired
    private IClassroomStudentWriteDataService classroomStudentWriteDataService;
    @Autowired
    private IClassroomTeacherWriteDataService classroomTeacherWriteDataService;
    @Autowired
    private IClassroomExercisesQuestionService classroomExercisesQuestionService;

    @Autowired
    private ZhipuAIConfig zhipuAIConfig;
    @Autowired
    private AIUtil aiUtil;
    @Autowired
    private IStudentAICallService studentAICallService;

    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;

    @Override
    public List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        Sort sort = Sort.by(Sort.Direction.ASC, "createTime");
        return classroomExercisesStudentRecordRepository.findAll(Example.of(record), sort);
    }

    @Override
    public ClassroomExercisesStudentRecord save(ClassroomExercisesStudentRecord studentRecord) {
        if (studentRecord.getId() == null) {
            studentRecord.setCreateTime(new Date());
        }
        ClassroomExercisesStudentRecord exercisesStudentRecord = classroomExercisesStudentRecordRepository.save(studentRecord);
        if (studentRecord.getStudentWriteDataList() != null && studentRecord.getStudentWriteDataList().size() > 0) {
            List<ClassroomStudentWriteData> studentWriteDataList = studentRecord.getStudentWriteDataList();
            for (ClassroomStudentWriteData studentWriteData : studentWriteDataList) {
                studentWriteData.setStudentRecordId(studentRecord.getId());
                classroomStudentWriteDataService.save(studentWriteData);
            }

        }
        return exercisesStudentRecord;
    }

    @Override
    public List<ClassroomExercisesStudentRecord> selectByClassroomExercisesIdAndClass(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        record.setClassId(classId);
        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration", "createTime");
        List<ClassroomExercisesStudentRecord> recordList = classroomExercisesStudentRecordRepository.findAll(Example.of(record), sort);
        for (ClassroomExercisesStudentRecord studentRecord : recordList) {
            List<ClassroomStudentWriteData> writeDataList = classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            studentRecord.setStudentWriteDataList(writeDataList);
        }
        return recordList;
    }

    @Override
    public void endAllAnswer(ExerciseWriteData exerciseWriteData) {
        Date now = new Date();
        List<ClassroomExercisesStudentRecord> recordList = new ArrayList<>();
        Long classroomExercisesId = exerciseWriteData.getClassroomExercisesId();
        Long classId = exerciseWriteData.getClassId();
        if (classroomExercisesId == 0) {
            Specification<ClassroomExercises> specification = new Specification<ClassroomExercises>() {
                @Override
                public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    List<Predicate> list = new ArrayList<>();
                    if (classId != null) {
                        Predicate condition1 = criteriaBuilder.like(root.get("classIds").as(String.class), "%" + classId + "%");
                        list.add(condition1);
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                    Predicate condition2 = criteriaBuilder.like(root.get("homeworkName").as(String.class), "%" + sdf.format(new Date()) + "堂课互动" + "%");
                    list.add(condition2);
                    Predicate[] p = new Predicate[list.size()];
                    return criteriaBuilder.and(list.toArray(p));
                }
            };
            Sort sort = Sort.by(Sort.Direction.DESC, "id", "publishTime", "createTime");
            List<ClassroomExercises> exercisesList = classroomExercisesRepository.findAll(specification, sort);
            if (exercisesList.size() > 0) {
                classroomExercisesId = exercisesList.get(0).getId();
            }
            recordList = selectByClassAndDate(classroomExercisesId, classId, now);
        } else {
            recordList = selectByClassroomExercisesIdAndClass(classroomExercisesId, classId);
        }
        List<StudentWriteDto> writeDtos = exerciseWriteData.getStudentWriteList();
        for (ClassroomExercisesStudentRecord record : recordList) {
            if (record.getEndFlag() == null || record.getEndFlag().equals("0")) {
                record.setEndFlag(1);
                record.setEndTime(now);
            }
            if (record.getStartTime() != null) {
                record.setAnswerDuration(now.getTime() - record.getStartTime().getTime());
            }
            for (StudentWriteDto studentWriteDto : writeDtos) {
                if (Long.compare(studentWriteDto.getStudentId(), record.getStudentId()) == 0) {
                    record.setStudentWriteDataList(studentWriteDto.getStudentWriteRecordList());

                    this.save(record);
                }
            }
            classroomExercisesStudentRecordRepository.save(record);

        }
        if (exerciseWriteData.getTeacherWriteRecords() != null && exerciseWriteData.getTeacherWriteRecords().size() > 0) {
            Optional<ClassroomExercises> optional = classroomExercisesRepository.findById(exerciseWriteData.getClassroomExercisesId());
            if (optional != null && optional.isPresent()) {
                ClassroomExercises exercises = optional.get();
                exercises.setTeacherWriteRecords(exerciseWriteData.getTeacherWriteRecords());
                classroomExercisesRepository.save(exercises);
            }

        }
        if(exerciseWriteData.getTeacherWriteDto()!=null){
            for(ClassroomTeacherWriteData teacherWriteData:exerciseWriteData.getTeacherWriteDto().getTeacherWriteDataList()){
                teacherWriteData.setClassroomExercisesId(classroomExercisesId);
                teacherWriteData.setCreateTime(new Date());
                classroomTeacherWriteDataService.save(teacherWriteData);
            }
        }
        final Long exercisesId = classroomExercisesId;
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            List<ClassroomExercisesStudentRecord> studentRecordList = selectByClassroomExercisesIdAndClass(exercisesId, classId);
            for (ClassroomExercisesStudentRecord record : studentRecordList) {
                aiParseWriteStrucRecord(record.getId());
            }

            return "异步-OK";

        });
        Thread thread = new Thread(futureTask);
        thread.start();
    }

    @Override
    public List<StudentWriteDto> getLiveStreamtRecord(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        if (classId != null) {
            record.setClassId(classId);
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration", "createTime");
        List<ClassroomExercisesStudentRecord> recordList = classroomExercisesStudentRecordRepository.findAll(Example.of(record), sort);
        List<StudentWriteDto> studentWriteDtoList = new ArrayList<>();
        for (ClassroomExercisesStudentRecord studentRecord : recordList) {
            StudentWriteDto studentWriteDto = new StudentWriteDto();
            studentWriteDto.setStudentId(studentRecord.getStudentId());
            studentWriteDto.setStudentName(studentRecord.getStudentName());
            List<ClassroomStudentWriteData> writeDataList = classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            studentWriteDto.setStudentWriteRecordList(writeDataList);
            studentWriteDtoList.add(studentWriteDto);
        }
        return studentWriteDtoList;
    }

    @Override
    public List<ClassroomExercisesStudentRecord> getClassInteractRecord(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        if (classId != null) {
            record.setClassId(classId);
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration", "createTime");
        List<ClassroomExercisesStudentRecord> recordList = classroomExercisesStudentRecordRepository.findAll(Example.of(record), sort);
        for (ClassroomExercisesStudentRecord studentRecord : recordList) {
            List<ClassroomStudentWriteData> writeDataList = classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            studentRecord.setStudentWriteDataList(writeDataList);
        }
        return recordList;
    }



    public List<ClassroomExercisesStudentRecord> selectByClassAndDate(Long classroomExercisesId, Long classId, Date date) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        record.setClassId(classId);
        Specification<ClassroomExercisesStudentRecord> specification = new Specification<ClassroomExercisesStudentRecord>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();

                if (classroomExercisesId != null) {
                    Predicate condition1 = criteriaBuilder.equal(root.get("classroomExercisesId"), classroomExercisesId);
                    list.add(condition1);
                }

                if (classId != null) {
                    Predicate condition2 = criteriaBuilder.equal(root.get("classId"), classId);
                    list.add(condition2);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();

                try {
                    Predicate condition3 = null;
                    String dateStr = sdf.format(date);
                    if (StringUtils.isNotEmpty(dateStr)) {
                        Date startDate1 = sdf.parse(dateStr);
                        calendar.setTime(startDate1);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Date endDate1 = calendar.getTime();
                        condition3 = criteriaBuilder.between(root.<Date>get("createTime"), startDate1, endDate1);
                        list.add(condition3);
                    }


                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        List<ClassroomExercisesStudentRecord> recordList = classroomExercisesStudentRecordRepository.findAll(specification,sort);
        for(ClassroomExercisesStudentRecord studentRecord:recordList){
            List<ClassroomStudentWriteData> writeDataList=classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            studentRecord.setStudentWriteDataList(writeDataList);
        }
        return recordList;
    }

    @Override
    public void aiParseWriteRecord(Long studentRecordId) {
        Optional<ClassroomExercisesStudentRecord> optional=classroomExercisesStudentRecordRepository.findById(studentRecordId);
        if(optional==null||!optional.isPresent()){
            return;
        }
        ClassroomExercisesStudentRecord studentRecord = optional.get();
        try {
            List<ClassroomExercisesQuestion> questionList = classroomExercisesQuestionService.selectQuestionList(studentRecord.getClassroomExercisesId());
            List<ClassroomStudentWriteData> writeDataList=classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            if(writeDataList==null||writeDataList.size()<=0){
                return;
            }
            BufferedImage image = WritingDataRenderer.drawWritingData(writeDataList.get(0).getStudentsWriteRecords(), 794, 1123);
            String imageUrl = "随堂检测-"+studentRecord.getClassroomExercisesId()+"-"+studentRecord.getStudentName()+".png";
            CoordinateImageGenerator.saveImage(image,imageUrl);
            //ZhipuAIImageAnalysisUtil util=zhipuAIConfig.zhipuAIImageAnalysisUtil();
            //QianWenAIUtil util = new QianWenAIUtil();
            AIUtil util  = aiUtil.getAIUtil();
            if("qianwen".equals(util.getAiName())){
                util = (QianWenAIUtil)  util;
            }else {
                util = (ZhipuAIImageAnalysisUtil) util;
            }
            String prompt = "请根据试题和学生书写笔记，批阅学生作答结果，试题如下：\n";
            for(ClassroomExercisesQuestion question:questionList){
                String questionContent = new String(Base64.getDecoder().decode(question.getQuestionContent()));
                String answer = new String(Base64.getDecoder().decode(question.getQuestionContent()));
                String titleStr = question.getTitleNumber() +".  "+questionContent + " 标准答案："+answer+" \n";
                prompt = prompt + titleStr;
            }
            prompt = prompt + " 请输出题号、学生作答文字内容、批阅结果，格式如 1.  作答:A ，批阅:正确。";
            String aiText=util.analyzeImage(imageUrl,prompt);
            //System.out.println("学生书写答案："+aiText);
            if(aiText!=null) {
                String text = aiText;
                if(text.contains("<|begin_of_box|>")){
                    text = text.substring(text.indexOf("<|begin_of_box|>") + 16);
                }
                if(text.contains("<|end_of_box|>")){
                    text = text.substring(0, text.indexOf("<|end_of_box|>"));
                }

                List<ClassroomExercisesStudentAnswer> studentAnswerList = new ArrayList<>();
                if ((questionList.size()<=2 &&(text.contains("1. ")||text.contains("1、 ")||text.contains(":1")))
                        || ((text.contains("1. ")||text.contains("1、 ")||text.contains(":1"))
                        && (text.contains("2. ")||text.contains("2、 ")||text.contains(":1"))
                        && (text.contains("3. ")||text.contains("3、 ")||text.contains(":1")))) {
                    for (int i = 0; i < questionList.size(); i++) {
                        try {
                            ClassroomExercisesQuestion question = questionList.get(i);
                            if (question.getTitleNumber() != null && (text.contains(question.getTitleNumber() + ". ") || text.contains(question.getTitleNumber() + "、 "))) {
                                Integer nextTitleNumber = question.getTitleNumber()+1;
                                String answerStr = null;
                                if (text.contains(question.getTitleNumber() + ". ")&&text.contains(nextTitleNumber + ". ")) {
                                    answerStr = text.substring(text.indexOf(question.getTitleNumber() + ". ") + 2, text.indexOf(nextTitleNumber + ". "));
                                } else if (text.contains(question.getTitleNumber() + "、 ")&&text.contains(nextTitleNumber + "、 ")) {
                                    answerStr = text.substring(text.indexOf(question.getTitleNumber() + "、 ") + 2, text.indexOf(nextTitleNumber + "、 "));
                                } else if (text.contains(question.getTitleNumber() + ". ")) {
                                    answerStr = text.substring(text.indexOf(question.getTitleNumber() + ". ") + 2);
                                } else if (text.contains(question.getTitleNumber() + "、 ")) {
                                    answerStr = text.substring(text.indexOf(question.getTitleNumber() + "、 ") + 2);
                                }else {
                                    continue;
                                }
                                //System.out.println("题答案："+answerStr);
                                String studAnswer = "";
                                String piyue="";
                                if(answerStr.contains("作答")&&answerStr.contains("批阅")){
                                    studAnswer = answerStr.substring(answerStr.indexOf("作答")+3,answerStr.indexOf("批阅"));
                                    piyue = answerStr.substring(answerStr.indexOf("批阅"));

                                }else {
                                    continue;
                                }
                                ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                                answer.setTitleNumber(question.getTitleNumber());
                                answer.setStudentAnswer(studAnswer);
                                if(piyue.contains("正确")){
                                    answer.setRightFlag(1);
                                }else{
                                    answer.setRightFlag(0);
                                }
                                studentAnswerList.add(answer);

                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                if (studentAnswerList.size() > 0) {
                    for (ClassroomExercisesStudentAnswer answer : studentAnswerList) {
                        for (ClassroomExercisesQuestion question : questionList) {
                            if (answer.getTitleNumber().compareTo(question.getTitleNumber()) == 0) {
                                answer.setExerciseQuestionId(question.getId());
                                answer.setStudentId(studentRecord.getStudentId() + "");
                                answer.setStudentName(studentRecord.getStudentName());
                                answer.setClassroomExercisesId(studentRecord.getClassroomExercisesId());
                                answer.setClassId(studentRecord.getClassId());
                                answer.setClassName(studentRecord.getClassName());
                                answer.setExercisesStudentRecordId(studentRecord.getId());
                                answer.setQuestionContent(question.getQuestionContent());
                                answer.setAnswer(question.getAnswer());
                                answer.setSubject(question.getSubject());
                                answer.setKnowledgePoint(question.getKnowledgePoint());
                                answer.setCreateTime(new Date());
                                classroomExercisesStudentAnswerRepository.save(answer);
                            }

                        }
                    }
                }
            }
            File file = new File(imageUrl);
            file.delete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void aiParseWriteStrucRecord(Long studentRecordId) {

        Optional<ClassroomExercisesStudentRecord> optional = classroomExercisesStudentRecordRepository.findById(studentRecordId);
        if (optional == null || !optional.isPresent()) {
            return;
        }
        ClassroomExercisesStudentRecord studentRecord = optional.get();
        List<ClassroomExercisesStudentAnswer> studentAnswerList = new ArrayList<>();

        try {
            List<ClassroomExercisesQuestion> questionList = classroomExercisesQuestionService.selectQuestionList(studentRecord.getClassroomExercisesId());
            List<ClassroomStudentWriteData> writeDataList = classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            if (writeDataList == null || writeDataList.size() <= 0) {
                return;
            }
            BufferedImage image = WritingDataRenderer.drawWritingData(writeDataList.get(0).getStudentsWriteRecords(), 794, 1123);
            String imageUrl = "随堂检测-" + studentRecord.getClassroomExercisesId() + "-" + studentRecord.getStudentName() + ".png";
            CoordinateImageGenerator.saveImage(image, imageUrl);

            String base64Str = encodeImageToBase64(imageUrl);

            List<Media> medias = new ArrayList<Media>();
            Media media = Media.builder().mimeType(MediaType.IMAGE_JPEG).data(base64Str).build();
            medias.add(media);
            /*AI处理*/
            String userMessage = "结构化输出";
            ZhiPuAIAgent.TopicJudgeReport topicReport = studentAICallService.obtainTeacherJudgeAnswer(userMessage, medias);
            //System.out.println("学生书写答案：" + topicReport);


            List<ZhiPuAIAgent.SubJudgeQuestions> answers = topicReport.answers();
            if (!CollectionUtils.isEmpty(answers)) {
                for (ZhiPuAIAgent.SubJudgeQuestions temp : answers) {
                    ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                    answer.setTitleNumber(Integer.valueOf(temp.question_id()));
                    answer.setStudentAnswer(temp.answer_text().toArray().toString());
                    String correct = temp.is_correct();

                    if ("true".equalsIgnoreCase(correct)) {
                        answer.setRightFlag(1);
                    } else {
                        answer.setRightFlag(0);
                    }
                    studentAnswerList.add(answer);
                }
            }

            if (studentAnswerList.size() > 0) {
                for (ClassroomExercisesStudentAnswer answer : studentAnswerList) {
                    for (ClassroomExercisesQuestion question : questionList) {
                        if (answer.getTitleNumber().compareTo(question.getTitleNumber()) == 0) {
                            answer.setExerciseQuestionId(question.getId());
                            answer.setStudentId(studentRecord.getStudentId() + "");
                            answer.setStudentName(studentRecord.getStudentName());
                            answer.setClassroomExercisesId(studentRecord.getClassroomExercisesId());
                            answer.setClassId(studentRecord.getClassId());
                            answer.setClassName(studentRecord.getClassName());
                            answer.setExercisesStudentRecordId(studentRecord.getId());
                            answer.setQuestionContent(question.getQuestionContent());
                            answer.setAnswer(question.getAnswer());
                            answer.setSubject(question.getSubject());
                            answer.setKnowledgePoint(question.getKnowledgePoint());
                            answer.setCreateTime(new Date());
                            classroomExercisesStudentAnswerRepository.save(answer);
                        }

                    }
                }
            }

            File file = new File(imageUrl);
            file.delete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TeacherWriteDto getLiveStreamtRecordTeacher(Long classroomExercisesId) {
        TeacherWriteDto  teacherWriteDto = new TeacherWriteDto();
        List<ClassroomTeacherWriteData> writeDataList=classroomTeacherWriteDataService.findByClassroomExercisesId(classroomExercisesId);
        teacherWriteDto.setTeacherWriteDataList(writeDataList);
        if(writeDataList!=null&&writeDataList.size()>0){
            teacherWriteDto.setTeacherId(writeDataList.get(0).getTeacherId());
            teacherWriteDto.setTeacherName(writeDataList.get(0).getTeacherName());
        }
        return teacherWriteDto;
    }

    /**
     * 将图片编码为Base64字符串
     *
     * @param imagePath 图片路径（支持本地文件路径或HTTP URL）
     * @return Base64编码后的图片数据
     * @throws IOException 读取异常
     */
    private String encodeImageToBase64(String imagePath) throws IOException {
        // 规范化URL格式，将反斜杠替换为正斜杠，确保http://格式正确
        String normalizedPath = imagePath;
        if (!normalizedPath.startsWith("http://") && !normalizedPath.startsWith("https://")) {
            normalizedPath = imagePath.replace("\\", "/")
                    .replace("http:/", "http://");
        }
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            // 处理网络图片
            URL url = new URL(normalizedPath);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                return Base64Utils.encodeToString(bytes);
            }
        } else {
            // 处理本地文件
            File file = new File(normalizedPath);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                return Base64Utils.encodeToString(bytes);
            }
        }
    }
}
