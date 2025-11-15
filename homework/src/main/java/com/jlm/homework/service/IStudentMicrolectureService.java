package com.jlm.homework.service;


import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.StudentMicrolecture;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface IStudentMicrolectureService {
    void createStudentMicrolecture(Microlecture microlecture);

    void save(StudentMicrolecture studentMicrolecture);

    StudentMicrolecture selectByMicrolectureAndStudent(Long microlectureId, Long studentId);

    void updateById(StudentMicrolecture studentMicrolecture);

    Page<StudentMicrolecture> page(Integer pageNum, Integer pageSize, Long microlectureId, String microlecturename, Long studentId, String studentName, Integer status,Integer searchType);

    Page<StudentMicrolecture> recordPage(Integer pageNum, Integer pageSize, Long microlectureId,Long studentId);
}
