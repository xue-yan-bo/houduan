package com.jlm.homework.service;

import com.jlm.homework.entity.StudentFeedback;
import org.springframework.data.domain.Page;


public interface IStudentFeedbackService {
    void save(StudentFeedback studentFeedback);
    StudentFeedback findById(long id);
    Page<StudentFeedback> findPage(Integer pageNum, Integer pageSize, StudentFeedback  studentFeedback);
}
