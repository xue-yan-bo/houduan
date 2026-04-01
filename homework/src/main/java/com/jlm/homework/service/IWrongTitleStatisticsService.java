package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.entity.WrongTitleStatistics;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public interface IWrongTitleStatisticsService {
    void createWrongTitleStatistics(Long homeworkPublishId,Long classId);

    List<WrongTitleStatistics> getWrongTitleStatisticses(Long homeworkPublishId,Long classId);

    Page<WrongTitleStatistics> getPage(Integer pageNum, Integer pageSize, WrongTitleStatistics wrongTitleStatistics, Date startTime, Date endTime);

    void updateClassWrongBook(WrongTitleStatistics wrongTitleStatistics);

    void deleteClassWrong(Long id);
}
