package com.jlm.homework.controller;

import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.service.ITeacherAttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "老师考勤记录", description = "老师考勤记录相关接口")
@RestController
@RequestMapping("/api/teacher-attendance-record")
public class TeacherAttendanceRecordController {
    @Autowired
    private ITeacherAttendanceRecordService studentAttendanceRecordService;
    /**
     * 开始考勤
     * @param classId
     * @param schoolId
     * @return
     */
    @GetMapping("/startAttendance")
    public TeacherAttendanceRecord startAttendance(Long classId,Long schoolId,String subject){
        return studentAttendanceRecordService.startAttendance(classId,schoolId,subject);
    }

    /**
     * 结束考勤
     * @param attendanceRecordId
     * @return
     */
    @GetMapping("/endAttendance")
    public TeacherAttendanceRecord endAttendance(Long attendanceRecordId){
        return studentAttendanceRecordService.endAttendance(attendanceRecordId);
    }

    /**
     * 老师考勤列表查询
     * @param teacherAttendanceRecord
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "老师考勤列表查询")
    public Page<TeacherAttendanceRecord> queryList(@RequestParam(defaultValue = "1")Integer pageNum,
             @RequestParam(defaultValue = "10") Integer pageSize,
             TeacherAttendanceRecord  teacherAttendanceRecord
    ){
        Page<TeacherAttendanceRecord> list = studentAttendanceRecordService.queryList(pageNum,pageSize,teacherAttendanceRecord);
        return list;
    }

}
