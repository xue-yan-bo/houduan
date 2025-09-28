package com.jlm.homework.controller;

import com.jlm.homework.entity.StudentSignRecord;
import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.service.IStudentSignRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
    public StudentSignRecord sign(@RequestBody StudentSignRecord studentSignRecord){
        return studentSignRecordService.sign(studentSignRecord);
    }
    /**
     * 学生签订列表查询
     * @param attendanceRecordId
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "根据老师考勤ID查询学生签订列表查询")
    public Page<StudentSignRecord> queryList(@RequestParam(defaultValue = "1")Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           Long attendanceRecordId
    ){
        Page<StudentSignRecord> list = studentSignRecordService.queryList(pageNum,pageSize,attendanceRecordId);
        return list;
    }

}
