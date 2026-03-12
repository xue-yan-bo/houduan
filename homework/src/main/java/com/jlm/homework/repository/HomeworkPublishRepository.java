package com.jlm.homework.repository;

import com.jlm.homework.entity.HomeworkPublish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
public interface HomeworkPublishRepository extends JpaRepository<HomeworkPublish, Long>, JpaSpecificationExecutor<HomeworkPublish> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE homework_publish h SET h.publish_status = ?2 WHERE h.id = ?1 ", nativeQuery = true)
    void updatePublishStatus(Long homeworkPublishId,Integer publishStatus);
}
