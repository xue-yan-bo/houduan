package com.jlm.homework.service;

import com.jlm.homework.dto.AccuracyDto;
import com.jlm.homework.dto.AverageAccuracyDto;
import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.springframework.data.domain.Page;


import java.util.List;

public interface IStudentsHomeworkNewService {
    void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish);

    List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest);

    StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework);

    Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest);

    List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject, Long classId, String startDate);

    Page<StudentsHomeworkNew> getStudentsHomeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    AccuracyDto getAverageAccuracyStatistics(String subject, Long classId, String startDate, String endDate);

    StudentsHomeworkNew getById(Long id);
}
