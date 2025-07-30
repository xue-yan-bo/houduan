package com.jlm.homework.service;

import com.jlm.homework.entity.QuestionType;
import org.springframework.data.domain.Page;

public interface IQuestionTypeService {
    Long create(QuestionType questionType);

    QuestionType getById(Long id);

    QuestionType update(QuestionType questionType);

    Page<QuestionType> selectList(Integer pageNum, Integer pageSize, QuestionType questionType);

    void deleteById(Long id);
}
