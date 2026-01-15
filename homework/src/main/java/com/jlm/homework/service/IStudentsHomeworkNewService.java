package com.jlm.homework.service;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsWriteRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

public interface IStudentsHomeworkNewService {
    void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish);

    List<StudentsHomeworkSimpleDTO> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest);

    StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework);

    Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest);

    Page<StudentsHomeworkNew> getClassHomeworkStatistics(Integer pageNum, Integer pageSize, String subject, Long classId, String startDate, String endDate);

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

    void saveWriteRecords(Long studentId,Long homeworkId, String type,Integer pageN, List<StudentsWriteRecord> studentsWriteRecords,Boolean isFinish);

    void saveStartTime(Long homeworkId);

    void emend(Long studentsHomeworkId);

    List<HomeWork2Board> getEmendHomeWork2Board(String subject,Long studentId);


    StudentsHomeworkNew audit(StudentsHomeworkNew studentsHomework);

    StudentHomeworkDto homeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    void saveFeedbackRecords(Long studentId,String subject ,List<StudentsWriteRecord> studentsFeedbackRecords);

    String aIaudit(Long studentsHomeworkId);
    /*AI�ṹ��*/
    String aIauditStruc(Long studentsHomeworkId);


    Long getSubmitNumByHomeworkPublishId(Long id);

    void saveErrorTitleRecords(Long studentId, String subject, List<StudentsWriteRecord> uploadErrorTitleRecords);

    StudentsHomeworkNew appSubmit(StudentsHomeworkNew studentsHomework);

    StudentsHomeworkNew appEmendSubmit(StudentsHomeworkNew studentsHomework);

    String aIauditEmend(Long studentsHomeworkId);

    String aIauditEmendStruc(Long studentsHomeworkId);


    Page<StudentsHomeworkSimpleDTO> getEmendPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    List<StudentsHomeworkSimpleDTO> findAllSimpleDTOBySpecification(Specification<StudentsHomeworkNew> specification);

    public void updateByPublishId(Long homeworkPublishId,String homeworkName, String topicImagesStr, Date deadline, Long dailyPracticeld, String dailyPracticeName, String dailyPracticePreview, String chapter, String knowledgePoint,Integer submitStatus);

    void updateSubmietNull(Long homeworkPublishId);

    void saveStudentsCopybookRecords(Long studentId,Long recordId,Integer pageN, List<StudentsWriteRecord> studentsCopybookRecords,Boolean isFinish);

    List<Copybook2Board> getCopybookBoards(Long studentId);
}
