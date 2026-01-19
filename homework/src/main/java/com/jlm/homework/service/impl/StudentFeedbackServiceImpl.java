package com.jlm.homework.service.impl;

import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.entity.StudentFeedback;
import com.jlm.homework.repository.StudentFeedbackRepository;
import com.jlm.homework.service.IStudentFeedbackService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.annotation.Reference;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentFeedbackServiceImpl implements IStudentFeedbackService {
    @Resource
    private StudentFeedbackRepository studentFeedbackRepository;
    @Autowired
    private UserService userService;

    @Override
    public void save(StudentFeedback studentFeedback) {
        studentFeedbackRepository.save(studentFeedback);
    }

    @Override
    public StudentFeedback findById(long id) {
        Optional<StudentFeedback> optional=studentFeedbackRepository.findById(id);
        if(optional!=null&&optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public Page<StudentFeedback> findPage(Integer pageNum, Integer pageSize, StudentFeedback studentFeedback) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        if(studentFeedback.getSchoolId()==null){
            studentFeedback.setSchoolId(userService.getCurrentSchoolId());
        }
        return studentFeedbackRepository.findAll(Example.of(studentFeedback),pageable);
    }
}
