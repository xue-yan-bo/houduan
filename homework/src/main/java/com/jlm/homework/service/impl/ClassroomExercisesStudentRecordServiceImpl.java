package com.jlm.homework.service.impl;

import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.ExerciseWriteData;
import com.jlm.homework.dto.StudentWriteDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IClassroomExercisesStudentRecordService;
import com.jlm.homework.service.IClassroomStudentWriteDataService;
import com.jlm.homework.util.CoordinateImageGenerator;
import com.jlm.homework.util.ImageOverlayUtil;
import com.jlm.homework.util.WritingDataRenderer;
import com.jlm.homework.util.ZhipuAIImageAnalysisUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private IClassroomExercisesQuestionService classroomExercisesQuestionService;

    @Autowired
    private ZhipuAIConfig zhipuAIConfig;

    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;
    @Override
    public List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        Sort sort = Sort.by(Sort.Direction.ASC, "createTime");
        return classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
    }

    @Override
    public ClassroomExercisesStudentRecord save(ClassroomExercisesStudentRecord studentRecord) {
        if(studentRecord.getId()==null){
            studentRecord.setCreateTime(new Date());
        }
        ClassroomExercisesStudentRecord exercisesStudentRecord=classroomExercisesStudentRecordRepository.save(studentRecord);
        if(studentRecord.getStudentWriteDataList()!=null&&studentRecord.getStudentWriteDataList().size()>0){
            List<ClassroomStudentWriteData> studentWriteDataList=studentRecord.getStudentWriteDataList();
            for(ClassroomStudentWriteData studentWriteData:studentWriteDataList){
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
        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
        for(ClassroomExercisesStudentRecord studentRecord:recordList){
            List<ClassroomStudentWriteData> writeDataList=classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
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
        if(classroomExercisesId == 0){
            Specification<ClassroomExercises> specification = new Specification<ClassroomExercises>() {
                @Override
                public Predicate toPredicate(Root<ClassroomExercises> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    List<Predicate> list =  new ArrayList<>();
                    if(classId!=null){
                        Predicate condition1 = criteriaBuilder.like(root.get("classIds").as(String.class), "%"+classId+"%");
                        list.add(condition1);
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                    Predicate condition2 = criteriaBuilder.like(root.get("homeworkName").as(String.class), "%"+sdf.format(new Date())+"堂课互动"+"%");
                    list.add(condition2);
                    Predicate[] p =  new Predicate[list.size()];
                    return criteriaBuilder.and(list.toArray(p));
                }
            };
            Sort sort = Sort.by(Sort.Direction.DESC, "id","publishTime","createTime");
            List<ClassroomExercises> exercisesList=classroomExercisesRepository.findAll(specification,sort);
            if(exercisesList.size()>0){
                classroomExercisesId =  exercisesList.get(0).getId();
            }
            recordList = selectByClassAndDate(classroomExercisesId, classId,now);
        }else  {
            recordList = selectByClassroomExercisesIdAndClass(classroomExercisesId, classId);
        }
        List<StudentWriteDto> writeDtos = exerciseWriteData.getStudentWriteList();
        for (ClassroomExercisesStudentRecord record : recordList) {
            if(record.getEndFlag()==null||record.getEndFlag().equals("0")) {
                record.setEndFlag(1);
                record.setEndTime(now);
            }
            if(record.getStartTime()!=null){
                record.setAnswerDuration(now.getTime() - record.getStartTime().getTime());
            }
            for(StudentWriteDto studentWriteDto : writeDtos){
                if(Long.compare(studentWriteDto.getStudentId(),record.getStudentId())==0){
                    record.setStudentWriteDataList(studentWriteDto.getStudentWriteRecordList());

                    this.save(record);
                }
            }
            classroomExercisesStudentRecordRepository.save(record);

        }
        if(exerciseWriteData.getTeacherWriteRecords()!=null&&exerciseWriteData.getTeacherWriteRecords().size()>0){
            Optional<ClassroomExercises> optional=classroomExercisesRepository.findById(exerciseWriteData.getClassroomExercisesId());
            if(optional!=null&&optional.isPresent()){
                ClassroomExercises exercises=optional.get();
                exercises.setTeacherWriteRecords(exerciseWriteData.getTeacherWriteRecords());
                classroomExercisesRepository.save(exercises);
            }

        }
        final Long exercisesId =classroomExercisesId;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                List<ClassroomExercisesStudentRecord> studentRecordList = selectByClassroomExercisesIdAndClass(exercisesId, classId);
                for (ClassroomExercisesStudentRecord record : studentRecordList) {
                    aiParseWriteRecord(record.getId());
                }

                return 123;
            }
        });

        System.out.println("Doing something else while waiting for the result...");
        Integer result = null; // 获取结果，如果结果尚未计算完成，将阻塞等待
        try {
            result = future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Result: " + result);
        executor.shutdown();
    }

    @Override
    public List<StudentWriteDto> getLiveStreamtRecord(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        if(classId!=null){
            record.setClassId(classId);
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
        List<StudentWriteDto> studentWriteDtoList = new ArrayList<>();
        for(ClassroomExercisesStudentRecord studentRecord:recordList){
            StudentWriteDto  studentWriteDto = new StudentWriteDto();
            studentWriteDto.setStudentId(studentRecord.getStudentId());
            studentWriteDto.setStudentName(studentRecord.getStudentName());
            List<ClassroomStudentWriteData> writeDataList=classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
            studentWriteDto.setStudentWriteRecordList(writeDataList);
            studentWriteDtoList.add(studentWriteDto);
        }
        return studentWriteDtoList;
    }

    @Override
    public List<ClassroomExercisesStudentRecord> getClassInteractRecord(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        if(classId!=null){
            record.setClassId(classId);
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
        for(ClassroomExercisesStudentRecord studentRecord:recordList){
            List<ClassroomStudentWriteData> writeDataList=classroomStudentWriteDataService.findByStudentRecordId(studentRecord.getId());
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
                List<Predicate> list =  new ArrayList<>();

                if(classroomExercisesId!=null){
                    Predicate condition1 = criteriaBuilder.equal(root.get("classroomExercisesId"), classroomExercisesId);
                    list.add(condition1);
                }

                if(classId!=null){
                    Predicate condition2 = criteriaBuilder.equal(root.get("classId"), classId);
                    list.add(condition2);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();

                try {
                    Predicate condition3 = null;
                    String dateStr = sdf.format(date);
                    if(StringUtils.isNotEmpty(dateStr)){
                        Date startDate1 = sdf.parse(dateStr);
                        calendar.setTime(startDate1);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Date endDate1 = calendar.getTime();
                        condition3 = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
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
            ZhipuAIImageAnalysisUtil aiImageAnalysisUtil=zhipuAIConfig.zhipuAIImageAnalysisUtil();
            String aiText=aiImageAnalysisUtil.recognizeTextInImage(imageUrl);
            System.out.println("学生书写答案："+aiText);
            if(aiText!=null){
                String text=aiText.substring(aiText.indexOf("<|begin_of_box|>")+16,aiText.indexOf("<|end_of_box|>"));
                List<ClassroomExercisesStudentAnswer> studentAnswerList = new ArrayList<>();
                Pattern pattern = Pattern.compile("(\\d+)([^\\d]*)(\\d+)");
                Matcher matcher = pattern.matcher(text);
                if(text.length()==questionList.size()){
                    for(int i=0;i<questionList.size();i++){
                        ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                        answer.setTitleNumber(i+1);
                        answer.setStudentAnswer(text.substring(i,i+1));
                        studentAnswerList.add(answer);
                    }
                }else if(matcher.find()){
                    Pattern pattern1 = Pattern.compile("(\\d+)\\s*([A-Z])");
                    Matcher matcher1 = pattern1.matcher(text);

                    while (matcher1.find()) {
                        String key = matcher1.group(1);
                        String value = matcher1.group(2);
                        ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                        answer.setTitleNumber(Integer.getInteger(key));
                        answer.setStudentAnswer(value);
                        studentAnswerList.add(answer);
                    }
                }else if(text.contains(" ")){
                    List<String> answerList = Arrays.stream(text.split(" ")).toList();
                    for (int i = 0; i < answerList.size(); i++) {
                        ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                        answer.setTitleNumber(i+1);
                        answer.setStudentAnswer(answerList.get(i));
                        studentAnswerList.add(answer);
                    }
                }else if(text.contains("\\n")){
                    String[] lines = text.split("\\r?\\n");
                    for (int i=0;i<lines.length;i++) {
                        String line = lines[i];
                        // 去除空格并检查是否有字母
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && trimmed.length() >= 1) {
                            ClassroomExercisesStudentAnswer answer = new ClassroomExercisesStudentAnswer();
                            answer.setTitleNumber(i+1);
                            answer.setStudentAnswer(trimmed);
                            studentAnswerList.add(answer);
                        }
                    }
                }else {
                    System.out.println("无法解析学生所写");
                }
                if(studentAnswerList.size()>0){
                    for(ClassroomExercisesStudentAnswer answer:studentAnswerList){
                        for(ClassroomExercisesQuestion question:questionList){
                            if(answer.getTitleNumber().compareTo(question.getTitleNumber())==0){
                                answer.setExerciseQuestionId(question.getId());
                                answer.setStudentId(studentRecord.getStudentId()+"");
                                answer.setStudentName(studentRecord.getStudentName());
                                answer.setClassroomExercisesId(studentRecord.getClassroomExercisesId());
                                answer.setClassId(studentRecord.getClassId());
                                answer.setClassName(studentRecord.getClassName());
                                answer.setExercisesStudentRecordId(studentRecord.getId());
                                answer.setQuestionContent(question.getQuestionContent());
                                answer.setAnswer(question.getAnswer());
                                answer.setSubject(question.getSubject());
                                answer.setKnowledgePoint(question.getKnowledgePoint());
                                if(question.getAnswer()!=null&&question.getAnswer().equals(answer.getStudentAnswer())){
                                    answer.setRightFlag(1);
                                }
                                answer.setCreateTime(new Date());
                                classroomExercisesStudentAnswerRepository.save(answer);
                            }

                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
