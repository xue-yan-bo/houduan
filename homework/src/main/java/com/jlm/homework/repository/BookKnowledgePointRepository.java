package com.jlm.homework.repository;

import com.jlm.homework.entity.BookKnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookKnowledgePointRepository extends JpaRepository<BookKnowledgePoint, Long> {
}
