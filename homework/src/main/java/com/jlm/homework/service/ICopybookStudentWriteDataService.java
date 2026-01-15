package com.jlm.homework.service;


import com.jlm.homework.entity.CopybookStudentWriteData;

import java.util.List;

public interface ICopybookStudentWriteDataService {
    void save(CopybookStudentWriteData copybookStudentWriteData);

    List<CopybookStudentWriteData> findByStudentRecordId(Long studentRecordId);
}
