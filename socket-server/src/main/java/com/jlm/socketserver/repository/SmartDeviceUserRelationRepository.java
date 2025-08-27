package com.jlm.socketserver.repository;


import com.jlm.socketserver.entity.SmartDeviceUserRelation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartDeviceUserRelationRepository extends JpaRepository<SmartDeviceUserRelation,Long> {
}
