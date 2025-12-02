package com.jlm.homework.service;


import com.jlm.homework.entity.Microlecture;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface IMicrolectureService  {
    Page<Microlecture> selectPage(Integer pageNum,Integer pageSize,Long schoolId, String name, Long teacherId, String teacherName, Long gradeId, String gradeName, Long classId, String className, String subject, String chapter,String knowledgePoint,  LocalDateTime startTime, LocalDateTime endTime);

    void save(Microlecture microlecture);

    void delete(Long microlectureId);
}
