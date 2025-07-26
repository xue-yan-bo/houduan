package com.jlm.homework.repository;

import com.jlm.homework.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
}
