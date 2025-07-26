package com.jlm.homework.service;

import com.jlm.homework.entity.KnowledgePoint;
import org.springframework.data.domain.Page;

public interface IKnowledgePointService {
    Long create(KnowledgePoint knowledgePoint);

    KnowledgePoint getById(Long id);

    KnowledgePoint update(KnowledgePoint knowledgePoint);

    Page<KnowledgePoint> selectList(Integer pageNum, Integer pageSize, KnowledgePoint knowledgePoint);

    void deleteById(Long id);
}
