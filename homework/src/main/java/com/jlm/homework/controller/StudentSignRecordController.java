package com.jlm.homework.controller;

import com.jlm.homework.entity.StudentSignRecord;
import com.jlm.homework.repository.StudentSignRecordRepository;
import com.jlm.homework.service.IStudentSignRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学生签到记录", description = "学生签到记录相关接口")
@RestController
@RequestMapping("/api/student-sign-record")
public class StudentSignRecordController {
    @Autowired
    private IStudentSignRecordService studentSignRecordService;
    /**
     * 组卷
     */
    @PostMapping("/sign")
    public StudentSignRecord sign(StudentSignRecord studentSignRecord){
        return studentSignRecordService.sign(studentSignRecord);
    }
}
