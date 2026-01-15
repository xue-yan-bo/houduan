package com.jlm.homework.repository;

import com.jlm.homework.entity.CopybookStudentWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CopybookStudentWriteDataRepository  extends JpaRepository<CopybookStudentWriteData,Long> {
}
