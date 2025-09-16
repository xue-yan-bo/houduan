package com.jlm.homework.service.impl;

import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.QuestionBankRepository;
import com.jlm.homework.service.IQuestionBankService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QuestionBankImpl implements IQuestionBankService {
    @Resource
    private QuestionBankRepository questionBankRepository;
    @Override
    public Long create(QuestionBank questionBank) {
        questionBank =questionBankRepository.save(questionBank);
        return questionBank.getId();
    }

    @Override
    public QuestionBank getById(Long id) {
        Optional<QuestionBank> optional=questionBankRepository.findById(id);
        if(optional.isEmpty()){
            return null;
        }
        return optional.get();
    }

    @Override
    public QuestionBank update(QuestionBank questionBank) {
        return questionBankRepository.save(questionBank);
    }

    @Override
    public Page<QuestionBank> selectList(Integer pageNum, Integer pageSize, QuestionBank questionBank) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        questionBank=QuestionBank.hanldKong(questionBank);
        return questionBankRepository.findAll(Example.of(questionBank), pageable);
    }

    @Override
    public void deleteById(Long id) {
        questionBankRepository.deleteById(id);
    }
}
