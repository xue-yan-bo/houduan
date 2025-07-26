package com.jlm.homework.service;

import com.jlm.homework.entity.QuestionBank;
import org.springframework.data.domain.Page;

public interface IQuestionBankService {
    Long create(QuestionBank questionBank);

    QuestionBank getById(Long id);

    QuestionBank update(QuestionBank questionBank);

    Page<QuestionBank> selectList(Integer pageNum, Integer pageSize, QuestionBank questionBank);

    void deleteById(Long id);
}
