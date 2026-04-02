package com.jlm.homework.repository;

import com.jlm.homework.entity.WrongGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WrongGroupItemRepository extends JpaRepository<WrongGroupItem, Long> {
    
    /**
     * 根据错题组ID获取错题组题目
     * @param wrongGroupId 错题组ID
     * @return 错题组题目列表
     */
    List<WrongGroupItem> findByWrongGroupId(Long wrongGroupId);
}