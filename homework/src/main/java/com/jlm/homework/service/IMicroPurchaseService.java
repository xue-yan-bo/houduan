package com.jlm.homework.service;

import com.jlm.homework.entity.MicroPurchase;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IMicroPurchaseService {
    Long create(MicroPurchase microPurchase);

    MicroPurchase getById(Long id);

    MicroPurchase update(MicroPurchase microPurchase);

    Page<MicroPurchase> selectList(Integer pageNum, Integer pageSize, MicroPurchase microPurchase);

    void deleteById(Long id);

    List<MicroPurchase> listByStudentId(Long studentId);
}
