package com.jlm.homework.service;

import com.jlm.homework.entity.TeacherAttendanceRecord;

public interface ITeacherAttendanceRecordService {
    TeacherAttendanceRecord startAttendance(Long classId,Long schoolId);

    TeacherAttendanceRecord endAttendance(Long attendanceRecordId);
}
