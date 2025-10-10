package com.jlm.homework.service;

import com.jlm.homework.entity.TeacherAttendanceRecord;
import org.springframework.data.domain.Page;

public interface ITeacherAttendanceRecordService {
    TeacherAttendanceRecord startAttendance(Long classId,Long schoolId,String subject);

    TeacherAttendanceRecord endAttendance(Long attendanceRecordId);

    Page<TeacherAttendanceRecord> queryList(Integer pageNum, Integer pageSize, TeacherAttendanceRecord teacherAttendanceRecord);
}
