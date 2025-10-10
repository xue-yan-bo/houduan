package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomVideoRecord;
import org.springframework.data.domain.Page;

public interface IClassroomVideoRecordService {
    Long create(ClassroomVideoRecord classroomVideoRecord);

    ClassroomVideoRecord getById(Long id);

    ClassroomVideoRecord update(ClassroomVideoRecord classroomVideoRecord);

    Page<ClassroomVideoRecord> selectList(Integer pageNum, Integer pageSize, ClassroomVideoRecord classroomVideoRecord);

    void deleteById(Long id);
}
