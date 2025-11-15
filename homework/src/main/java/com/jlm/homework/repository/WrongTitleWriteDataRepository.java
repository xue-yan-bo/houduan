package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongTitleWriteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrongTitleWriteDataRepository extends JpaRepository<WrongTitleWriteData, Long>, JpaSpecificationExecutor<WrongTitleWriteData> {
}
