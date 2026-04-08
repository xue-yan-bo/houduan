package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomStudentWriteData;

import java.util.List;

public interface IClassroomStudentWriteDataService {
    void save(ClassroomStudentWriteData classroomStudentWriteData);

    List<ClassroomStudentWriteData> findByStudentRecordId(Long studentRecordId);
    
    List<ClassroomStudentWriteData> findByStudentRecordIds(List<Long> studentRecordIds);

    void saveAll(List<ClassroomStudentWriteData> list);
}
