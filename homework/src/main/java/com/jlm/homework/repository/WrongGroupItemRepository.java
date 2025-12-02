package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrongGroupItemRepository extends JpaRepository<WrongGroupItem,Long> , JpaSpecificationExecutor<WrongGroupItem> {
}
