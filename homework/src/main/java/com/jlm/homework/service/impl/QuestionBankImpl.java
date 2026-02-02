package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.QuestionBankRepository;
import com.jlm.homework.service.IQuestionBankService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        questionBank=QuestionBank.hanldKong(questionBank);
        QuestionBank finalQuestionBank = questionBank;
        Specification<QuestionBank> specification = new Specification<QuestionBank>() {

            @Override
            public Predicate toPredicate(Root<QuestionBank> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(StringUtils.isNotEmpty(finalQuestionBank.getQuestionType())) {
                        Predicate condition = criteriaBuilder.like(root.get("questionType"), "%"+finalQuestionBank.getQuestionType()+"%");
                        list.add(condition);
                    }
                    if(finalQuestionBank.getDifficulty()!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("difficulty"), finalQuestionBank.getDifficulty());
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(finalQuestionBank.getKnowledgePoint())) {
                        Predicate condition = criteriaBuilder.like(root.get("knowledgePoint"), "%"+finalQuestionBank.getKnowledgePoint()+"%");
                        list.add(condition);
                    }
                    if(finalQuestionBank.getDifficulty()!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("difficulty"), finalQuestionBank.getDifficulty());
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(finalQuestionBank.getSubject())) {
                        Predicate condition = criteriaBuilder.equal(root.get("subject"), finalQuestionBank.getSubject().trim());
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(finalQuestionBank.getGrade())) {
                        Predicate condition = criteriaBuilder.equal(root.get("grade"), finalQuestionBank.getGrade());
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(finalQuestionBank.getSemester())) {
                        Predicate condition = criteriaBuilder.equal(root.get("semester"), finalQuestionBank.getSemester());
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(finalQuestionBank.getChapter())) {
                        Predicate condition = criteriaBuilder.like(root.get("chapter"), "%"+finalQuestionBank.getChapter().trim()+"%");
                        list.add(condition);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }

        };
        return questionBankRepository.findAll(specification, pageable);
    }

    @Override
    public void deleteById(Long id) {
        questionBankRepository.deleteById(id);
    }
}
