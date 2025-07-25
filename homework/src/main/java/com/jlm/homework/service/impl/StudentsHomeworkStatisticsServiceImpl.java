package com.jlm.homework.service.impl;

import com.jlm.homework.entity.StudentsHomeworkStatistics;
import com.jlm.homework.repository.StudentsHomeworkStatisticsRepository;
import com.jlm.homework.service.IStudentsHomeworkStatisticsService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentsHomeworkStatisticsServiceImpl implements IStudentsHomeworkStatisticsService {
    @Resource
    private StudentsHomeworkStatisticsRepository studentsHomeworkStatisticsRepository;
    @Override
    public StudentsHomeworkStatistics getStudentsHomeworkStatistics(Long homeworkPublishId,Long classId) {
        StudentsHomeworkStatistics studentsHomeworkStatistics = new StudentsHomeworkStatistics();
        studentsHomeworkStatistics.setHomeworkPublishId(homeworkPublishId);
        studentsHomeworkStatistics.setClassId(classId);
        Example<StudentsHomeworkStatistics> example=Example.of(studentsHomeworkStatistics);
        Optional<StudentsHomeworkStatistics> optional= studentsHomeworkStatisticsRepository.findOne(example);
        return optional.get();
    }

    @Override
    public Long insertStudentHomeworkStatistics(StudentsHomeworkStatistics studentsHomeworkStatistics) {
        studentsHomeworkStatistics=studentsHomeworkStatisticsRepository.save(studentsHomeworkStatistics);
        return studentsHomeworkStatistics.getId();
    }


}
