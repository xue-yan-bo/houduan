package com.jlm.homework.service;

import com.jlm.homework.dto.FeedbackDto;
import com.jlm.homework.dto.StudentFeedbackReq;
import com.jlm.homework.entity.StudentFeedback;
import com.jlm.homework.entity.StudentsWriteRecord;
import org.springframework.data.domain.Page;

import java.util.List;


public interface IStudentFeedbackService {
    StudentFeedback save(StudentFeedback studentFeedback);
    StudentFeedback findById(long id);
    Page<StudentFeedback> findPage(Integer pageNum, Integer pageSize, StudentFeedbackReq studentFeedback);

    List<FeedbackDto> getFeedbackDto(Long schoolId, String feedbackTimeStart, String feedbackTimeEnd);

    StudentFeedback findByStudentAndSubject(Long studentId, String subject);

    void saveMoreWriteRecords(Long feedbackId, Long studentId, List<StudentsWriteRecord> studentsFeedbackRecords);
}
