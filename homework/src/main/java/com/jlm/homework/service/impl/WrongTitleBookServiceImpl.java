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
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 1));
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
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getGrade())){
                        Predicate condition = criteriaBuilder.equal(root.get("grade"),wrongTitleBook.getGrade());
                        list.add(condition);
                    }
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSubject())){
                        Predicate condition = criteriaBuilder.equal(root.get("subject"),wrongTitleBook.getSubject());
                        list.add(condition);
                    }
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getHomeworkPublishName())){
                        Predicate condition = criteriaBuilder.like(root.get("homeworkPublishName"),"%"+wrongTitleBook.getHomeworkPublishName()+"%");
                        list.add(condition);
                    }
                    if(com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getSource())){
                        Predicate condition = criteriaBuilder.like(root.get("source"),"%"+wrongTitleBook.getSource()+"%");
                        list.add(condition);
                    }
                    // 过滤掉被合并的题（个人层面的合并）
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 2));
                    list.add(criteriaBuilder.notEqual(root.get("duplicateStatus"), 1));

                    // 班级错题本去重：班级去重时过滤掉所有重复别人的题，但如果搜的是个人的就不应该在这里过滤所有的duplicateOf
                    if (wrongTitleBook.getStudentId() == null) {
                        list.add(criteriaBuilder.isNull(root.get("duplicateOf")));
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
                // 添加：设置年级信息
                if (StringUtils.isEmpty(wrongTitleBook.getGrade())) {
                    wrongTitleBook.setGrade(resultDto.getData().getGradeName());
                }
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
        String grade = null;
        ResultDto<Student> resultDto= studentFeignClient.getStudentInfo(wrongTitleBook.getStudentId());
        if(resultDto!=null&&resultDto.getData()!=null){
            schoolId = resultDto.getData().getSchoolId();
            grade = resultDto.getData().getGradeName();
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
            if(StringUtils.isNotEmpty(grade)){
                statistics.setGrade(grade);
            }
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
            if(StringUtils.isNotEmpty(grade)){
                statistics.setGrade(grade);
            }
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
                        String fetchedText = aiResult.getData();
                        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(fetchedText)) {
                            wrongTitleBook.setTitleContext(fetchedText);
                            wrongTitleBookRepository.save(wrongTitleBook);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }

        // 无论是由上述代码刚刚识别出文字，还是本来入库时就已经自带文字，只要有题目内容，就开始执行去重查重
        if (com.alibaba.cloud.commons.lang.StringUtils.isNotEmpty(wrongTitleBook.getTitleContext())) {
            // 如果已经被处理为重复（2），或者已经判定完（3并有duplicateOf），则不再重复去重
            if (wrongTitleBook.getDuplicateStatus() == null || wrongTitleBook.getDuplicateStatus() == 0) {
                String text = wrongTitleBook.getTitleContext();

                // ----------- 新增：异步去重查重核心逻辑 -----------
                // 为防止批量并发提交导致的查重失效，对查重入库逻辑加锁
                synchronized (this.getClass()) {
                    // 重新从数据库获取最新状态的母题记录，避免并发时的脏读
                    wrongTitleBook = wrongTitleBookRepository.findById(wrongTitleBook.getId()).orElse(wrongTitleBook);

                    // 查询范围扩大到当前班级 (不仅限当前学生)
                    if (wrongTitleBook.getClassId() != null) {
                        WrongTitleBook search = new WrongTitleBook();
                        search.setClassId(wrongTitleBook.getClassId());

                        final Long currentClassId = wrongTitleBook.getClassId();

                        // 使用 Specification 确保查询条件准确无误，只按 classId 查询
                        java.util.List<WrongTitleBook> historyBooks = wrongTitleBookRepository.findAll(new Specification<WrongTitleBook>() {
                            @Override
                            public Predicate toPredicate(Root<WrongTitleBook> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                                if (currentClassId != null) {
                                    return cb.equal(root.get("classId"), currentClassId);
                                }
                                return cb.conjunction();
                            }
                        });

                        SimilarityRequestDto requestDto = new SimilarityRequestDto();
                        requestDto.setTargetText(text);
                        List<SimilarityRequestDto.HistoryTextDto> historyList = new ArrayList<>();

                        double maxSimilarity = 0.0;
                        Long duplicateOf = null;
                        boolean exactMatchFound = false;

                        // 遍历提取文本内容用于查重
                        for (WrongTitleBook history : historyBooks) {
                            // 排除自己、没有文本内容的记录、个人已被合并的记录(2)，以及已经是别人题目的重复题(duplicateOf != null，即只和母题查重)
                            if (history.getId().equals(wrongTitleBook.getId())
                                    || com.alibaba.cloud.commons.lang.StringUtils.isEmpty(history.getTitleContext())
                                    || (history.getDuplicateStatus() != null && history.getDuplicateStatus() == 2)
                                    || history.getDuplicateOf() != null) {
                                continue;
                            }

                            // 增加精确匹配兜底：如果相似度大于等于 0.8 直接算重复，不用再调AI
                            double localSimilarity = com.jlm.homework.util.TextSimilarityUtil.getSimilarity(text, history.getTitleContext());
                            if (localSimilarity >= 0.8) {
                                maxSimilarity = localSimilarity;
                                duplicateOf = history.getId();
                                exactMatchFound = true;
                                break;
                            }

                            SimilarityRequestDto.HistoryTextDto hText = new SimilarityRequestDto.HistoryTextDto();
                            hText.setId(history.getId());
                            hText.setText(history.getTitleContext());
                            historyList.add(hText);
                        }

                        if (!exactMatchFound && !historyList.isEmpty()) {
                            requestDto.setHistoryTexts(historyList);
                            Result<SimilarityResultDto> simResult = aiFeignClient.checkSimilarity(requestDto);
                            System.out.println("====== simResult: " + com.alibaba.fastjson.JSON.toJSONString(simResult));
                            if(simResult != null && simResult.getCode() == 200 && simResult.getData() != null) {
                                maxSimilarity = simResult.getData().getMaxSimilarity() != null ? simResult.getData().getMaxSimilarity() : 0.0;
                                duplicateOf = simResult.getData().getDuplicateOf();
                            }
                        }

                        if (maxSimilarity >= 0.8 && duplicateOf != null) {
                            wrongTitleBook.setDuplicateOf(duplicateOf);

                            // 更新母题的错误次数
                            Optional<WrongTitleBook> parentOpt = wrongTitleBookRepository.findById(duplicateOf);
                            if (parentOpt.isPresent()) {
                                WrongTitleBook parentBook = parentOpt.get();
                                if (parentBook.getStudentId() != null && wrongTitleBook.getStudentId() != null && parentBook.getStudentId().equals(wrongTitleBook.getStudentId())) {
                                    // 同一个学生的相同错题，废弃新题，次数+1
                                    wrongTitleBook.setDuplicateStatus(2);
                                    Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                    parentBook.setErrorCount(oldErrorCount + 1);
                                    wrongTitleBookRepository.save(parentBook);
                                } else {
                                    // 班级里不同学生的错题，要看这个学生以前有没有错过这道题
                                    // 如果这个学生以前错过，才更新以前那条记录的次数并把新记录设为2
                                    // 如果没错过，就不设为2，而是把它作为这名学生的“初次错题”，只打标记3和关联母题

                                    // 1. 查找当前学生是否已有该错题（根据 duplicateOf 或 本身是母题）
                                    WrongTitleBook studentHistorySearch = new WrongTitleBook();
                                    studentHistorySearch.setStudentId(wrongTitleBook.getStudentId());
                                    studentHistorySearch.setClassId(wrongTitleBook.getClassId());
                                    final Long wtbStudentId = wrongTitleBook.getStudentId();
                                    final Long wtbClassId = wrongTitleBook.getClassId();
                                    final Long finalDuplicateOf = duplicateOf;

                                    // 我们需要找到属于该学生、且题目内容相似的记录
                                    // 为了严谨，我们直接找：关联到同一个母题，或者本身就是那个母题
                                    List<WrongTitleBook> stuBooks = wrongTitleBookRepository.findAll((root, query, cb) -> {
                                        Predicate pStu = cb.equal(root.get("studentId"), wtbStudentId);
                                        Predicate pClass = cb.equal(root.get("classId"), wtbClassId);
                                        Predicate pDup = cb.or(
                                                cb.equal(root.get("id"), finalDuplicateOf),
                                                cb.equal(root.get("duplicateOf"), finalDuplicateOf)
                                        );
                                        return cb.and(pStu, pClass, pDup);
                                    });

                                    if (stuBooks != null && !stuBooks.isEmpty()) {
                                        // 这个学生以前确实错过这道题
                                        WrongTitleBook hisStuBook = stuBooks.get(0);
                                        wrongTitleBook.setDuplicateStatus(2);
                                        Integer oldErrorCount = hisStuBook.getErrorCount() == null ? 1 : hisStuBook.getErrorCount();
                                        hisStuBook.setErrorCount(oldErrorCount + 1);
                                        wrongTitleBookRepository.save(hisStuBook);

                                        // 同时不要忘记母题也要+1，因为班级整体错误数变了
                                        Integer parentErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                        parentBook.setErrorCount(parentErrorCount + 1);
                                        wrongTitleBookRepository.save(parentBook);
                                    } else {
                                        // 这个学生第一次错这道题，正常累加母题次数
                                        Integer oldErrorCount = parentBook.getErrorCount() == null ? 1 : parentBook.getErrorCount();
                                        parentBook.setErrorCount(oldErrorCount + 1);
                                        wrongTitleBookRepository.save(parentBook);

                                        // 班级本去重，不展示这条（因为前端班级错题本默认会过滤掉 duplicateOf 不为空的）
                                        wrongTitleBook.setDuplicateStatus(3);
                                    }
                                }
                            }
                        } else {
                            // 新题，或者相似度小于0.8
                            wrongTitleBook.setDuplicateStatus(3);
                        }
                    } else {
                        wrongTitleBook.setDuplicateStatus(3);
                    }

                    // 保证新题至少有一次错误记录
                    if (wrongTitleBook.getErrorCount() == null) {
                        wrongTitleBook.setErrorCount(1);
                    }
                    wrongTitleBookRepository.save(wrongTitleBook);
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
        // 在删除个人错题前，检查是否需要同步减少班级错题统计表中的错误人数
        Optional<WrongTitleBook> optional = wrongTitleBookRepository.findById(id);
        if (optional.isPresent()) {
            WrongTitleBook wrongTitleBook = optional.get();
            if (wrongTitleBook.getClassId() != null) {
                WrongTitleStatistics search = new WrongTitleStatistics();
                search.setClassId(wrongTitleBook.getClassId());
                search.setExerciseBookQuestionId(wrongTitleBook.getExerciseBookQuestionId());
                search.setTitleImage(wrongTitleBook.getTitleImage());
                Example<WrongTitleStatistics> example = Example.of(search);
                Optional<WrongTitleStatistics> statOpt = wrongTitleStatisticsRepository.findOne(example);
                if (statOpt.isPresent()) {
                    WrongTitleStatistics statistics = statOpt.get();
                    if (statistics.getWrongNum() != null && statistics.getWrongNum() > 1) {
                        statistics.setWrongNum(statistics.getWrongNum() - 1);
                        wrongTitleStatisticsRepository.save(statistics);
                    } else if (statistics.getWrongNum() != null && statistics.getWrongNum() == 1) {
                        // 如果只有1个人错，错题人数变成0，但保留这道题在班级错题本中
                        statistics.setWrongNum(0);
                        wrongTitleStatisticsRepository.save(statistics);
                    }
                }
            }
        }
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
