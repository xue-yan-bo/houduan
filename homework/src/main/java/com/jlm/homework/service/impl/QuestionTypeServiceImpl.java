package com.jlm.homework.service.impl;

import com.jlm.homework.entity.QuestionType;
import com.jlm.homework.repository.QuestionTypeRepository;
import com.jlm.homework.service.IQuestionTypeService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class QuestionTypeServiceImpl implements IQuestionTypeService {
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Override
    public Long create(QuestionType questionType) {
        return questionTypeRepository.save(questionType).getId();
    }

    @Override
    public QuestionType getById(Long id) {
        return questionTypeRepository.findById(id).get();
    }

    @Override
    public QuestionType update(QuestionType questionType) {
        return questionTypeRepository.save(questionType);
    }

    @Override
    public Page<QuestionType> selectList(Integer pageNum, Integer pageSize, QuestionType questionType) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.ASC, "sort");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return questionTypeRepository.findAll(Example.of(questionType),pageable);
    }

    @Override
    public void deleteById(Long id) {
        questionTypeRepository.deleteById(id);
    }
}
