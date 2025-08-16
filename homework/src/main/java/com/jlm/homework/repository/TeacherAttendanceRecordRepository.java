package com.jlm.homework.repository;

import com.jlm.homework.entity.TeacherAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAttendanceRecordRepository extends JpaRepository<TeacherAttendanceRecord, Long> {
}
