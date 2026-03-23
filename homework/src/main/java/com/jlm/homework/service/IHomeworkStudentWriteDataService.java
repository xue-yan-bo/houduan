package com.jlm.homework.service;

import com.jlm.homework.entity.HomeworkStudentWriteData;

import java.util.List;
import java.util.Map;

public interface IHomeworkStudentWriteDataService {
    void save(HomeworkStudentWriteData homeworkStudentWriteData);

    List<HomeworkStudentWriteData> findByStudentRecordId(Long studentHomeworkId,String type);

    Map<Long, List<HomeworkStudentWriteData>> findByStudentRecordIds(List<Long> studentHomeworkIds, String type);

    void updateOffset(HomeworkStudentWriteData writeData);
}
