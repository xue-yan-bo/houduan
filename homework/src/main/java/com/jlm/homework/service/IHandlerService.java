package com.jlm.homework.service;

import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.dto.HomeWork2Board;
import com.jlm.homework.entity.StudentsWriteRecord;

import java.util.List;

public interface IHandlerService {
    List<HomeWork2Board> getHomeWork2Board(String subject, String date, Long studentId);

    void saveWriteRecords(Long studentId, Long homeworkId, String type, Integer pageN, List<StudentsWriteRecord> studentsWriteRecords, Boolean isFinish);

    void saveStartTime(Long homeworkId);

    List<HomeWork2Board> getEmendHomeWork2Board(String subject,Long studentId);

    void saveFeedbackRecords(Long studentId,String subject ,List<StudentsWriteRecord> studentsFeedbackRecords);

    void saveErrorTitleRecords(Long studentId, String subject, List<StudentsWriteRecord> uploadErrorTitleRecords);

    void saveStudentsCopybookRecords(Long studentId,Long recordId,Integer pageN, List<StudentsWriteRecord> studentsCopybookRecords,Boolean isFinish);

    List<Copybook2Board> getCopybookBoards(Long studentId);

}
