package com.jlm.homework.repository;

import com.jlm.homework.entity.SmartDeviceUserRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmartDeviceUserRelationRepository extends JpaRepository<SmartDeviceUserRelation,Long> {
    List<SmartDeviceUserRelation> findByUserIdInAndUserType(List<String> userIds, Integer userType);
}
