package com.jlm.homework.service;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsWriteRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IStudentsHomeworkNewService {
    void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish);

    List<StudentsHomeworkSimpleDTO> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest);

    StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework);

    Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest);

    Page<StudentsHomeworkNew> getClassHomeworkStatistics(Integer pageNum, Integer pageSize, String subject, Long classId, String startDate, String endDate);

    Page<StudentsHomeworkNew> getStudentsHomeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    StudentsHomeworkNew getById(Long id);

    void endStudentsHomework(HomeworkPublish homeworkPublish);


    List<HomeWork2Board> getHomeWork2Board(String subject,String date,Long studentId);

    void saveWriteRecords(Long studentId,Long homeworkId, String type,Integer pageN, List<StudentsWriteRecord> studentsWriteRecords,Boolean isFinish);

    void saveStartTime(Long homeworkId);

    void emend(Long studentsHomeworkId);

    List<HomeWork2Board> getEmendHomeWork2Board(String subject,Long studentId);


    StudentsHomeworkNew audit(StudentsHomeworkNew studentsHomework);

    StudentHomeworkDto homeworkPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    Long getSubmitNumByHomeworkPublishId(Long id);

    Map<Long, Long> getSubmitNumMapByHomeworkPublishIds(List<Long> homeworkPublishIds);

    StudentsHomeworkNew appSubmit(StudentsHomeworkNew studentsHomework);

    StudentsHomeworkNew appEmendSubmit(StudentsHomeworkNew studentsHomework);

    Page<StudentsHomeworkSimpleDTO> getEmendPage(Integer pageNum, Integer pageSize, StudentsHomeworkNew studentsHomework);

    List<StudentsHomeworkSimpleDTO> findAllSimpleDTOBySpecification(Specification<StudentsHomeworkNew> specification);

    public void updateByPublishId(Long homeworkPublishId,String homeworkName, String topicImagesStr, Date deadline, Long dailyPracticeld, String dailyPracticeName, String dailyPracticePreview, String chapter, String knowledgePoint,Integer submitStatus);

    void updateSubmietNull(Long homeworkPublishId);

}
