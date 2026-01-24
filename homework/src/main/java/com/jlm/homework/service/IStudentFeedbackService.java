package com.jlm.homework.service;

import com.jlm.homework.dto.FeedbackDto;
import com.jlm.homework.dto.StudentFeedbackReq;
import com.jlm.homework.entity.StudentFeedback;
import org.springframework.data.domain.Page;

import java.util.List;


public interface IStudentFeedbackService {
    void save(StudentFeedback studentFeedback);
    StudentFeedback findById(long id);
    Page<StudentFeedback> findPage(Integer pageNum, Integer pageSize, StudentFeedbackReq studentFeedback);

    List<FeedbackDto> getFeedbackDto(Long schoolId, String feedbackTimeStart, String feedbackTimeEnd);
}
