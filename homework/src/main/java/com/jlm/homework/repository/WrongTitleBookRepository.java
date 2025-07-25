package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongTitleBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrongTitleBookRepository extends JpaRepository<WrongTitleBook, Long> , JpaSpecificationExecutor<WrongTitleBook> {
}
