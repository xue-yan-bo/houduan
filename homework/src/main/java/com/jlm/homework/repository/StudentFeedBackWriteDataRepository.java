package com.jlm.homework.repository;

import com.jlm.homework.entity.StudentFeedBackWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentFeedBackWriteDataRepository  extends JpaRepository<StudentFeedBackWriteData, Long>, JpaSpecificationExecutor<StudentFeedBackWriteData> {
}
