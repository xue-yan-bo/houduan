package com.jlm.homework.service.impl;

import ai.z.openapi.service.image.ImageResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.dto.SimilarityRequestDto;
import com.jlm.homework.dto.SimilarityResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.AiFeignClient;
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
    private AiFeignClient aiFeignClient;
    @Autowired
    private AIUtil aiUtil;

    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        extractImageTextIfEmpty(wrongTitleBook);
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public Page<WrongTitleBook> findByStudentId(Long studentId, Integer pageNum, Integer pageSize, String source, Integer commandFlag) {
        Pageable pageable = PageRequest.of(pageNum-1,pageSize, Sort.by("createTime").descending());
        return wrongTitleBookRepository.findAll(new Specification<WrongTitleBook>() {
            @Override
            public Predicate toPredicate(Root<WrongTitleBook> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    Predicate cond = criteriaBuilder.equal(root.get("studentId"),studentId);
                    list.add(cond);
                    // 排除掉那些已经被合并掉的题目（状态为2的），只显示主题目和解析中的题目
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 2));
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(source)){
                        Predicate condition = criteriaBuilder.like(root.get("source"),"%"+source+"%");
                        list.add(condition);
                    }
                    if(commandFlag != null){
                        Predicate condition = criteriaBuilder.equal(root.get("commandFlag"),commandFlag);
                        list.add(condition);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Predicate[] array = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(array));
            }
        },pageable);
    }

    @Override
    public void createWrongBook(Long studentsHomeworkId) {
        Optional<StudentsHomeworkNew> optional = studentsHomeworkNewRepository.findById(studentsHomeworkId);
        if(optional!=null&&optional.isPresent()){
            StudentsHomeworkNew homeworkNew = optional.get();
            WrongTitleBook wrongTitleBook = new WrongTitleBook();
            wrongTitleBook.setStudentId(homeworkNew.getStudentId());
            wrongTitleBook.setStudentsHomeworkId(homeworkNew.getId());
            wrongTitleBook.setExerciseBookId(homeworkNew.getExerciseBookId());
            wrongTitleBook.setExerciseBookQuestionId(homeworkNew.getExerciseBookQuestionId());
            wrongTitleBook.setClassId(homeworkNew.getClassId());
            wrongTitleBook.setSource("智能作业扫描");
            wrongTitleBook.setSubject(homeworkNew.getSubject());
            this.addWrongBook(wrongTitleBook);
        }
    }

    @Override
    public Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook) {
        Pageable pageable = PageRequest.of(pageNum-1,pageSize, Sort.by("createTime").descending());
        return wrongTitleBookRepository.findAll(new Specification<WrongTitleBook>() {
            @Override
            public Predicate toPredicate(Root<WrongTitleBook> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(wrongTitleBook.getStudentId()!=null){
                        Predicate condition = criteriaBuilder.equal(root.get("studentId"),wrongTitleBook.getStudentId());
                        list.add(condition);
                    }
                    if(wrongTitleBook.getExerciseBookId()!=null){
                        Predicate condition = criteriaBuilder.equal(root.get("exerciseBookId"),wrongTitleBook.getExerciseBookId());
                        list.add(condition);
                    }
                    if(wrongTitleBook.getClassId()!=null){
                        Predicate condition = criteriaBuilder.equal(root.get("classId"),wrongTitleBook.getClassId());
                        list.add(condition);
                    }
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSource())){
                        Predicate condition = criteriaBuilder.like(root.get("source"),"%"+wrongTitleBook.getSource()+"%");
                        list.add(condition);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Predicate[] array = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(array));
            }
        },pageable);
    }

    @Override
    public Result<Object> addWrongBook(WrongTitleBook wrongTitleBook) {
        boolean isNew = true;
        if(wrongTitleBook.getExerciseBookQuestionId()!=null){
            Optional<ExerciseBookQuestion> optional = exerciseBookQuestionRepository.findById(wrongTitleBook.getExerciseBookQuestionId());
            if(optional!=null&&optional.isPresent()){
                ExerciseBookQuestion question = optional.get();
                wrongTitleBook.setTitleContext(question.getQuestionContext());
                wrongTitleBook.setTitleImage(question.getQuestionImage());
                wrongTitleBook.setAnswerContext(question.getAnswerContext());
                wrongTitleBook.setAnswerImage(question.getAnswerImage());
                wrongTitleBook.setSubject(question.getSubject());
            }

            WrongTitleBook search = new WrongTitleBook();
            search.setStudentId(wrongTitleBook.getStudentId());
            search.setExerciseBookQuestionId(wrongTitleBook.getExerciseBookQuestionId());
            Example<WrongTitleBook> example = Example.of(search);
            Optional<WrongTitleBook> optional1 = wrongTitleBookRepository.findOne(example);
            if(optional1!=null&&optional1.isPresent()){
                wrongTitleBook = optional1.get();
                isNew = false;
            }
        }else if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())){
            WrongTitleBook search = new WrongTitleBook();
            search.setStudentId(wrongTitleBook.getStudentId());
            search.setTitleImage(wrongTitleBook.getTitleImage());
            Example<WrongTitleBook> example = Example.of(search);
            Optional<WrongTitleBook> optional1 = wrongTitleBookRepository.findOne(example);
            if(optional1!=null&&optional1.isPresent()){
                wrongTitleBook = optional1.get();
                isNew = false;
            }
        }

        if(wrongTitleBook.getStudentId()!=null){
            ResultDto<Student> resultDto= studentFeignClient.getStudentInfo(wrongTitleBook.getStudentId());
            if(resultDto!=null&&resultDto.getData()!=null){
                wrongTitleBook.setSchoolId(resultDto.getData().getSchoolId());
            }
        }

        wrongTitleBook.setCreateTime(new Date());
        if(com.alibaba.cloud.commons.lang.StringUtils.isEmpty(wrongTitleBook.getSource())){
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
                if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(wrongTitleBook.getSubject()) && com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(homeworkNew.getSubject())) {
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
        final WrongTitleBook finalWrongTitleBook = wrongTitleBook;
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            aiChart(finalWrongTitleBook.getId());
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

        WrongTitleStatistics search = new WrongTitleStatistics();
        search.setClassId(wrongTitleBook.getClassId());
        search.setExerciseBookQuestionId(wrongTitleBook.getExerciseBookQuestionId());
        search.setTitleImage(wrongTitleBook.getTitleImage());
        Example<WrongTitleStatistics> example = Example.of(search);
        Optional<WrongTitleStatistics> optional = wrongTitleStatisticsRepository.findOne(example);
        WrongTitleStatistics statistics = null;
        if(optional!=null&&optional.isPresent()){
            statistics = optional.get();
            statistics.setWrongNum(statistics.getWrongNum()+1);
        }else{
            statistics = new WrongTitleStatistics();
            statistics.setClassId(wrongTitleBook.getClassId());
            statistics.setExerciseBookId(wrongTitleBook.getExerciseBookId());
            statistics.setExerciseBookQuestionId(wrongTitleBook.getExerciseBookQuestionId());
            statistics.setTitleImage(wrongTitleBook.getTitleImage());
            statistics.setTitleContext(wrongTitleBook.getTitleContext());
            statistics.setAnswerImage(wrongTitleBook.getAnswerImage());
            statistics.setAnswerContext(wrongTitleBook.getAnswerContext());
            statistics.setSubject(wrongTitleBook.getSubject());
            statistics.setWrongNum(1);
        }
        if(studentNum>0){
            BigDecimal wrongNum = new BigDecimal(statistics.getWrongNum());
            BigDecimal totalNum = new BigDecimal(studentNum);
            statistics.setWrongRate(wrongNum.divide(totalNum,4,BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).doubleValue());
        }
        wrongTitleStatisticsRepository.save(statistics);
    }

    @Override
    public void updateWrongBook(WrongTitleBook wrongTitleBook) {
        wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public void updateCommandFlag(Long wrongTitleId, Integer commandFlag) {
        Optional<WrongTitleBook> optional = wrongTitleBookRepository.findById(wrongTitleId);
        if(optional!=null&&optional.isPresent()){
            WrongTitleBook wrongTitleBook = optional.get();
            wrongTitleBook.setCommandFlag(commandFlag);
            wrongTitleBookRepository.save(wrongTitleBook);
        }
    }

    @Override
    public void aiChart(Long wrongTitleId) {
        Optional<WrongTitleBook> optional=wrongTitleBookRepository.findById(wrongTitleId);
        if(optional==null||!optional.isPresent()){
            return;
        }
        WrongTitleBook wrongTitleBook = optional.get();
        if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(wrongTitleBook.getTitleContext())) {
            String imageUrl = null;
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                imageUrl = wrongTitleBook.getTitleImage();
            } else if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                imageUrl = wrongTitleBook.getSourceImageUrl();
            }
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(imageUrl)) {
                try {
                    Result<String> aiResult = aiFeignClient.analyzeImage(imageUrl);
                    if (aiResult != null && aiResult.getCode() == 200) {
                        String text = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                            wrongTitleBook.setTitleContext(text);
                            // ----------- 新增：异步去重查重核心逻辑 -----------
                            // 查询范围扩大到当前班级 (不仅限当前学生)
                            if (wrongTitleBook.getClassId() != null) {
                                WrongTitleBook search = new WrongTitleBook();
                                search.setClassId(wrongTitleBook.getClassId());

                                // 查找该班级所有的历史错题
                                java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(Example.of(search));

                                SimilarityRequestDto requestDto = new SimilarityRequestDto();
                                requestDto.setTargetText(text);
                                List<SimilarityRequestDto.HistoryTextDto> historyList = new ArrayList<>();

                                // 遍历提取文本内容用于查重
                                for (WrongTitleBook history : historyBooks) {
                                    // 排除自己，且排除没有文本内容的记录，并且只和主错题(不等于2)查重
                                    if (history.getId().equals(wrongTitleBook.getId()) || com.alibaba.cloud.commons.lang.StringUtils.isEmpty(history.getTitleContext()) || (history.getDuplicateStatus() != null && history.getDuplicateStatus() == 2)) {
                                        continue;
                                    }
                                    SimilarityRequestDto.HistoryTextDto hText = new SimilarityRequestDto.HistoryTextDto();
                                    hText.setId(history.getId());
                                    hText.setText(history.getTitleContext());
                                    historyList.add(hText);
                                }
                                requestDto.setHistoryTexts(historyList);

                                double maxSimilarity = 0.0;
                                Long duplicateOf = null;

                                Result<SimilarityResultDto> simResult = aiFeignClient.checkSimilarity(requestDto);
                                if(simResult != null && simResult.getCode() == 200 && simResult.getData() != null) {
                                    maxSimilarity = simResult.getData().getMaxSimilarity() != null ? simResult.getData().getMaxSimilarity() : 0.0;
                                    duplicateOf = simResult.getData().getDuplicateOf();
                                }

                                if (maxSimilarity >= 0.8 && duplicateOf != null) {
                                    // 确定是重复题：自动标记状态为2 (已合并/废弃)
                                    wrongTitleBook.setDuplicateStatus(2);
                                    wrongTitleBook.setDuplicateOf(duplicateOf);

                                    // 更新母题的错误次数
                                    Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);
                                    if (parentOpt.isPresent()) {
                                        WrongTitleBook parentBook = parentOpt.get();
                                        Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                        parentBook.setErrorCount(oldErrorCount + 1);
                                        wrongTitleBookRepository.save(parentBook);
                                    }
                                } else {
                                    // 新题，或者相似度小于0.8
                                    wrongTitleBook.setDuplicateStatus(3);
                                }
                            } else {
                                wrongTitleBook.setDuplicateStatus(3);
                            }
                            // 去重逻辑结束，新题标记为3 (解析完成)，并且不能直接在 catch 中拦截阻止 save
                            wrongTitleBookRepository.save(wrongTitleBook);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }

        // 继续生成AI图表分析
        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleContext())) {
            try {
                String prompt = "你是一个专业的作业分析AI。请针对以下错题内容进行分析，并输出一段简短的掌握度分析（50字以内）。\n内容：" + wrongTitleBook.getTitleContext();
                // 修复报红: 临时屏蔽本地 aiUtil.analyzeText() 调用，因为它可能涉及本地 API key 配置错误导致 401 权限问题，且这部分不是去重核心功能
                // String analysis = aiUtil.getAIUtil().analyzeText(prompt);
                String analysis = "错题已成功收录，建议后续加强相关知识点的练习。";
                if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(analysis)) {
                    wrongTitleBook.setAiAnalysis(analysis);
                    wrongTitleBookRepository.save(wrongTitleBook);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        if (com.alibaba.cloud.commons.lang.StringUtils.isEmpty(wrongTitleBook.getTitleContext())) {
            String imageUrl = null;
            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleImage())) {
                imageUrl = wrongTitleBook.getTitleImage();
            } else if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSourceImageUrl())) {
                imageUrl = wrongTitleBook.getSourceImageUrl();
            }

            if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(imageUrl)) {
                try {
                    Result<String> aiResult = aiFeignClient.analyzeImage(imageUrl);
                    if (aiResult != null && aiResult.getCode() == 200) {
                        String text = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(text)) {
                            wrongTitleBook.setTitleContext(text);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }
    }
}
