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
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
    public Page<WrongTitleStatistics> getPage(Integer pageNum, Integer pageSize, WrongTitleStatistics wrongTitleBook) {
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
        return wrongTitleStatisticsRepository.findAll(Example.of(wrongTitleBook),pageable);
    }


}
