package com.jlm.homework.service;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsWriteRecord;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IStudentsHomeworkNewService {
    void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish);

    List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest);

    StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework);

    Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest);

    List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject, Long classId, String startDate,String endDate);

    Page<StudentsHomeworkNew> getStudentsHomeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    AccuracyDto getAverageAccuracyStatistics(String subject, Long classId, String startDate, String endDate);

    StudentsHomeworkNew getById(Long id);

    List<StudentChapterAccuracy> studentChapterStatistics(String subject, Long classId, String chapter);

    List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracy(String subject, Long classId, String startDate, String endDate);

    void endStudentsHomework(HomeworkPublish homeworkPublish);

    SchoolHomeworkData getSchoolHomeworkData(Long schoolId);

    EducHomeworkData getEducHomeworkData(Long educOrgId,Long schoolId,String schoolType);

    HomeworkStatisticsDto getHomeworkStatistics(String startDate, String endDate);

    List<HomeWork2Board> getHomeWork2Board(String subject,String date,Long studentId);

    void saveWriteRecords(Long studentId,Long homeworkId, String homeworkName,Integer pageN, List<StudentsWriteRecord> studentsWriteRecords,Boolean isFinish);

    void saveStartTime(Long homeworkId);
}
