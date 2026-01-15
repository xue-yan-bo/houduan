package com.jlm.homework.repository;

import com.jlm.homework.entity.CopybookStudentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CopybookStudentRecordRepository extends JpaRepository<CopybookStudentRecord, Long> , JpaSpecificationExecutor<CopybookStudentRecord> {
}
