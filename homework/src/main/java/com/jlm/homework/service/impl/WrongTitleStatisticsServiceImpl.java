package com.jlm.homework.service.impl;

import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.repository.WrongTitleStatisticsRepository;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WrongTitleStatisticsServiceImpl implements IWrongTitleStatisticsService {
    @Resource
    private WrongTitleStatisticsRepository wrongTitleStatisticsRepository;

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
        return wrongTitleStatisticsRepository.findAll(Example.of(wrongTitleBook),pageable);
    }

}
