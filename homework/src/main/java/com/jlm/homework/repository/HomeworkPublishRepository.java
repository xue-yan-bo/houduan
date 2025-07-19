package com.jlm.homework.repository;

import com.jlm.homework.entity.HomeworkPublish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeworkPublishRepository extends JpaRepository<HomeworkPublish, Long>, JpaSpecificationExecutor<HomeworkPublish> {
}
