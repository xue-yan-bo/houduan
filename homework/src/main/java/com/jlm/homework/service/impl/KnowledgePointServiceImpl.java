package com.jlm.homework.service.impl;

import com.jlm.homework.entity.KnowledgePoint;
import com.jlm.homework.repository.KnowledgePointRepository;
import com.jlm.homework.service.IKnowledgePointService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointServiceImpl implements IKnowledgePointService {
    @Resource
    private KnowledgePointRepository knowledgePointRepository;
    @Override
    public Long create(KnowledgePoint knowledgePoint) {
        knowledgePoint =knowledgePointRepository.save(knowledgePoint);
        return knowledgePoint.getId();
    }

    @Override
    public KnowledgePoint getById(Long id) {
        return knowledgePointRepository.findById(id).get();
    }

    @Override
    public KnowledgePoint update(KnowledgePoint knowledgePoint) {
        return knowledgePointRepository.save(knowledgePoint);
    }

    @Override
    public Page<KnowledgePoint> selectList(Integer pageNum, Integer pageSize, KnowledgePoint knowledgePoint) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        return knowledgePointRepository.findAll(Example.of(knowledgePoint),pageable);
    }

    @Override
    public void deleteById(Long id) {
        knowledgePointRepository.deleteById(id);
    }
}
