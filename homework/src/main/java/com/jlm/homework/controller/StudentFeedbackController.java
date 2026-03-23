package com.jlm.homework.controller;

import com.jlm.homework.dto.FeedbackDto;
import com.jlm.homework.dto.StudentFeedbackReq;
import com.jlm.homework.dto.StudentVo;
import com.jlm.homework.entity.QuestionType;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentFeedback;
import com.jlm.homework.service.IStudentFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学生反馈", description = "学生反馈的查询等")
@RestController
@RequestMapping("/api/student-feedback")
public class StudentFeedbackController {
    @Autowired
    private IStudentFeedbackService studentFeedbackService;
    /**
     * 学生反馈根据日期班级分类
     * @param
     * @return
     */
    @GetMapping("/getFeedbackDto")
    @Operation(summary = "学生反馈根据日期班级分类")
    public List<FeedbackDto> getFeedbackDto(Long schoolId,Long classId,String studentName,String feedbackTimeStart,String feedbackTimeEnd) {
        return studentFeedbackService.getFeedbackDto(schoolId,classId,studentName,feedbackTimeStart,feedbackTimeEnd);
    }
    /**
     * 分页查询
     * @param studentFeedback
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "学生反馈分页查询")
    public Page<StudentFeedback> queryList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            StudentFeedbackReq studentFeedback) {

        Page<StudentFeedback> list = studentFeedbackService.findPage(pageNum,pageSize, studentFeedback);

        return list;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询学生反馈详情")
    public StudentFeedback getById(@PathVariable Long id) {
        StudentFeedback feedback = studentFeedbackService.findById(id);
        return feedback;
    }
}
