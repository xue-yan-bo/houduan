package com.jlm.homework.service;


import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.StudentMicrolecture;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface IStudentMicrolectureService {
    void createStudentMicrolecture(Microlecture microlecture);

    void save(StudentMicrolecture studentMicrolecture);

    StudentMicrolecture selectByMicrolectureAndStudent(Long microlectureId, Long studentId);

    void updateById(StudentMicrolecture studentMicrolecture);

    Page<StudentMicrolecture> page(Integer pageNum, Integer pageSize, Long microlectureId, String microlectureName, Long studentId, String studentName, Integer status,String chapter,String knowledgePoint,Integer searchType);

    Page<StudentMicrolecture> recordPage(Integer pageNum, Integer pageSize, Long microlectureId,Long studentId);

    void deleteByMicrolectureId(Long microlectureId);

    List<StudentMicrolecture> findByMicrolectureId(Long microlectureId);

    void deleteById(Long id);
}
