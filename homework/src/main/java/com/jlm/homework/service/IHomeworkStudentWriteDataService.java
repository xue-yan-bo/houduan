package com.jlm.homework.service;

import com.jlm.homework.entity.HomeworkStudentWriteData;

import java.util.List;

public interface IHomeworkStudentWriteDataService {
    void save(HomeworkStudentWriteData homeworkStudentWriteData);

    List<HomeworkStudentWriteData> findByStudentRecordId(Long studentHomeworkId,String type);
}
