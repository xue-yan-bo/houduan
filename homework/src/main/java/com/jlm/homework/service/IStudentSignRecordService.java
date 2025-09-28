package com.jlm.homework.service;

import com.jlm.homework.entity.StudentSignRecord;
import org.springframework.data.domain.Page;

public interface IStudentSignRecordService {
    StudentSignRecord sign(StudentSignRecord studentSignRecord);

    Page<StudentSignRecord> queryList(Integer pageNum, Integer pageSize, Long attendanceRecordId);
}
