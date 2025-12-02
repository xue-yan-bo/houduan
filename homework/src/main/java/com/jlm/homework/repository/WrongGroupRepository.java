package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrongGroupRepository extends JpaRepository<WrongGroup,Long>, JpaSpecificationExecutor<WrongGroup> {
}
