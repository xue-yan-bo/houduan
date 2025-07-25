package com.jlm.homework.repository;

import com.jlm.homework.entity.StudentsHomeworkStatistics;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentsHomeworkStatisticsRepository extends JpaRepository<StudentsHomeworkStatistics,Long>, JpaSpecificationExecutor<StudentsHomeworkStatistics> {
}
