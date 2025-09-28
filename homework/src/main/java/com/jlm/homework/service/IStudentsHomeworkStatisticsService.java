package com.jlm.homework.service;

import com.jlm.homework.entity.StudentsHomeworkStatistics;

public interface IStudentsHomeworkStatisticsService {
    StudentsHomeworkStatistics getStudentsHomeworkStatistics(Long homeworkPublishId,Long classId);

    Long insertStudentHomeworkStatistics(StudentsHomeworkStatistics studentsHomeworkStatistics);

    void addStudentHomeworkStatistics(Long homeworkPublishId,Long classId);
}
