package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongTitleStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrongTitleStatisticsRepository extends JpaRepository<WrongTitleStatistics, Long> , JpaSpecificationExecutor<WrongTitleStatistics> {
}
