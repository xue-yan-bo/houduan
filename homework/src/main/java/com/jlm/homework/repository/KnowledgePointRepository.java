package com.jlm.homework.repository;

import com.jlm.homework.entity.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint,Long> {
}
