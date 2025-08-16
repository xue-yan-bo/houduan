package com.jlm.homework.controller;

import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.service.ITeacherAttendanceRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public TeacherAttendanceRecord startAttendance(Long classId,Long schoolId){
        return studentAttendanceRecordService.startAttendance(classId,schoolId);
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
}
