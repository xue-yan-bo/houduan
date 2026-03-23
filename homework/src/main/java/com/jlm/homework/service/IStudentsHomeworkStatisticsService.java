package com.jlm.homework.service;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.StudentsHomeworkStatistics;

import java.util.List;

public interface IStudentsHomeworkStatisticsService {
    StudentsHomeworkStatistics getStudentsHomeworkStatistics(Long homeworkPublishId,Long classId);

    Long insertStudentHomeworkStatistics(StudentsHomeworkStatistics studentsHomeworkStatistics);

    void addStudentHomeworkStatistics(Long homeworkPublishId,Long classId);

    SchoolHomeworkData getSchoolHomeworkData(Long schoolId);

    EducHomeworkData getEducHomeworkData(Long educOrgId, Long schoolId, String schoolType);

    HomeworkStatisticsDto getHomeworkStatistics(String startDate, String endDate);

    AccuracyDto getAverageAccuracyStatistics(String subject, Long classId, String startDate, String endDate);

    List<StudentChapterAccuracy> studentChapterStatistics(String subject, Long classId, String chapter);

    List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracy(String subject, Long classId, String startDate, String endDate);

}
