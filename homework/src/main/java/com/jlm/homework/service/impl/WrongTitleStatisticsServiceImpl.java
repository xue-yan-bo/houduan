package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.repository.WrongTitleBookRepository;
import com.jlm.homework.repository.WrongTitleStatisticsRepository;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.util.AIUtil;
import com.jlm.homework.util.ZhipuAIImageAnalysisUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WrongTitleStatisticsServiceImpl implements IWrongTitleStatisticsService {
    @Resource
    private WrongTitleStatisticsRepository wrongTitleStatisticsRepository;

    @Resource
    private WrongTitleBookRepository wrongTitleBookRepository;

    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;

    @Autowired
    private StudentFeignClient studentFeignClient;

    @Autowired
    private ZhipuAIConfig zhipuAIConfig;

    @Autowired
    private AIUtil aiUtil;



    public void createWrongTitleStatistics(Long homeworkPublishId,Long classId){
        WrongTitleBook search=new WrongTitleBook();
        search.setClassId(classId);
        search.setStudentsHomeworkId(search.getHomeworkPublishId());
        List<WrongTitleBook> wrongTitleBookList=wrongTitleBookRepository.findAll(Example.of(search));
        Map<String,Integer> wrongNumMap=new HashMap<>();
        List<WrongTitleStatistics> wrongTitleStatisticsList=new ArrayList<>();
        for(WrongTitleBook titleBook:wrongTitleBookList){
            String qkey = titleBook.getQuestionId() +":"+ titleBook.getTitleBigNo()+":"+titleBook.getTitleSmallNo();
            if(wrongNumMap.containsKey(qkey)){
                wrongNumMap.put(qkey,wrongNumMap.get(titleBook.getQuestionId())+1);
            }else {
                wrongNumMap.put(qkey,1);
                WrongTitleStatistics statistics=new WrongTitleStatistics();
                statistics.setClassId(titleBook.getClassId());
                statistics.setHomeworkPublishId(titleBook.getHomeworkPublishId());
                statistics.setHomeworkPublishName(titleBook.getHomeworkPublishName());
                statistics.setQuestionId(titleBook.getQuestionId());
                statistics.setSource(titleBook.getSource());
                statistics.setTitleImage(titleBook.getTitleImage());
                if(StringUtils.isEmpty(titleBook.getTitleImage())){
                    statistics.setTitleImage(titleBook.getSourceImageUrl());
                }

                statistics.setTitleBigNo(titleBook.getTitleBigNo());
                statistics.setTitleSmallNo(titleBook.getTitleSmallNo());
                statistics.setParse(titleBook.getParse());
                statistics.setPageNo(titleBook.getPageNo());
                statistics.setTitleContext(titleBook.getTitleContext());
                statistics.setCreateDate(new Date());
                wrongTitleStatisticsList.add(statistics);
            }
        }
        for(WrongTitleStatistics titleStatistics:wrongTitleStatisticsList){
            String qkey = titleStatistics.getQuestionId() +":"+ titleStatistics.getTitleBigNo()+":"+titleStatistics.getTitleSmallNo();
            Integer wrongNum =wrongNumMap.get(qkey);
            titleStatistics.setWrongStudentNum(wrongNum);
            StudentsHomeworkNew searchStu = new StudentsHomeworkNew();
            searchStu.setClassesId(titleStatistics.getClassId());
            searchStu.setHomeworkPublishId(titleStatistics.getHomeworkPublishId());
            //searchStu.setSubmitStatus(1);
            Long sum=studentsHomeworkNewRepository.count(Example.of(searchStu));
            titleStatistics.setAnswerTotal(Integer.valueOf(sum.toString()));
            Double wrongRate = 0d;
            if(sum!=null&&0!=sum&&wrongNum!=null){
                wrongRate = BigDecimal.valueOf(wrongNum).divide(BigDecimal.valueOf(sum),4,BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            titleStatistics.setWrongRate(wrongRate);
            wrongTitleStatisticsRepository.save(titleStatistics);
        }
    }

    @Override
    public List<WrongTitleStatistics> getWrongTitleStatisticses(Long homeworkPublishId, Long classId) {
        WrongTitleStatistics wrongTitleStatistics = new WrongTitleStatistics();
        wrongTitleStatistics.setHomeworkPublishId(homeworkPublishId);
        wrongTitleStatistics.setClassId(classId);
        Example example = Example.of(wrongTitleStatistics);
        List<WrongTitleStatistics> wrongTitleStatisticses=wrongTitleStatisticsRepository.findAll(example);
        return wrongTitleStatisticses;
    }

    @Override
    public Page<WrongTitleStatistics> getPage(Integer pageNum, Integer pageSize, WrongTitleStatistics wrongTitleBook, Date startTime, Date endTime) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);

        Specification<WrongTitleStatistics> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(wrongTitleBook!=null){
                if(StringUtils.isNotEmpty(wrongTitleBook.getHomeworkPublishName())){
                    predicates.add(cb.like(root.get("homeworkPublishName"), "%" + wrongTitleBook.getHomeworkPublishName() + "%"));
                }
                if(StringUtils.isNotEmpty(wrongTitleBook.getSource())){
                    predicates.add(cb.equal(root.get("source"), wrongTitleBook.getSource()));
                }
                if(StringUtils.isNotEmpty(wrongTitleBook.getSubject())){
                    predicates.add(cb.equal(root.get("subject"), wrongTitleBook.getSubject()));
                }
                if(StringUtils.isNotEmpty(wrongTitleBook.getGrade())){
                    predicates.add(cb.equal(root.get("grade"), wrongTitleBook.getGrade()));
                }
                if(wrongTitleBook.getSchoolId() != null && wrongTitleBook.getSchoolId() != 0){
                    predicates.add(cb.equal(root.get("schoolId"), wrongTitleBook.getSchoolId()));
                }
                if(wrongTitleBook.getClassId() != null){
                    predicates.add(cb.equal(root.get("classId"), wrongTitleBook.getClassId()));
                }
                if(wrongTitleBook.getHomeworkPublishId() != null){
                    predicates.add(cb.equal(root.get("homeworkPublishId"), wrongTitleBook.getHomeworkPublishId()));
                }
                if(wrongTitleBook.getQuestionId() != null){
                    predicates.add(cb.equal(root.get("questionId"), wrongTitleBook.getQuestionId()));
                }
            }

            if(startTime != null){
                predicates.add(cb.greaterThanOrEqualTo(root.get("createDate"), startTime));
            }
            if(endTime != null){
                predicates.add(cb.lessThanOrEqualTo(root.get("createDate"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return wrongTitleStatisticsRepository.findAll(spec, pageable);
    }

    @Override
    public void updateClassWrongBook(WrongTitleStatistics wrongTitleStatistics) {
        WrongTitleStatistics old=wrongTitleStatisticsRepository.findById(wrongTitleStatistics.getId()).orElse(null);
        if(StringUtils.isNotEmpty(wrongTitleStatistics.getTitleContext())){
            old.setTitleContext(wrongTitleStatistics.getTitleContext());
        }
        if(StringUtils.isNotEmpty(wrongTitleStatistics.getKnowledgePoint())){
            old.setKnowledgePoint(wrongTitleStatistics.getKnowledgePoint());
        }
        if(StringUtils.isNotEmpty(wrongTitleStatistics.getParse())){
            old.setParse(wrongTitleStatistics.getParse());
        }
        wrongTitleStatisticsRepository.save(wrongTitleStatistics);
    }

    @Override
    public void deleteClassWrong(Long id) {
        wrongTitleStatisticsRepository.deleteById(id);
    }



    @Override
    public void aiChart(Long id) {
        Optional<WrongTitleStatistics> optional = wrongTitleStatisticsRepository.findById(id);
        if (optional == null || !optional.isPresent()) {
            return;
        }
        WrongTitleStatistics wrongTitleStatistics = optional.get();
        if (StringUtils.isEmpty(wrongTitleStatistics.getTitleContext())) {
            String imageUrl = wrongTitleStatistics.getTitleImage();
            if (StringUtils.isEmpty(imageUrl)) {
                imageUrl = ""; // WrongTitleStatistics has no sourceImageUrl
            }
            if (StringUtils.isNotEmpty(imageUrl)) {
                try {
                    AIUtil aiUtils = aiUtil.getAIUtil();
                    String textPrompt = "请提取这张图片中的所有试题文字内容（包含题目、选项、解析等）。不论是文科（语文、历史、英语等）、理科还是美术等其他学科，请忠实还原图片中的所有文字。如果包含公式或特殊符号，请尽量使用Markdown或LaTeX语法表示。只返回提取的纯文字内容，不要输出诸如好的、提取的文字如下等任何废话。如果识别不到文字，只需返回空字符串。";
                    String text = aiUtils.analyzeImage(imageUrl, textPrompt);
                    if (StringUtils.isNotEmpty(text)) {
                        wrongTitleStatistics.setTitleContext(text);
                        wrongTitleStatisticsRepository.save(wrongTitleStatistics);
                    }
                } catch (Exception e) {
                    System.err.println("AI提取题目文字失败: " + e.getMessage());
                }
            }
        }
    }

}
