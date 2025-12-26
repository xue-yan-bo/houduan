package com.jlm.homework.service.impl;

import com.jlm.homework.entity.MicroPurchase;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.MicroPurchaseRepository;
import com.jlm.homework.service.IMicroPurchaseService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MicroPurchaseServiceImpl implements IMicroPurchaseService {
    @Resource
    private MicroPurchaseRepository microPurchaseRepository;

    @Override
    public Long create(MicroPurchase microPurchase) {
        microPurchase=microPurchaseRepository.save(microPurchase);
        return microPurchase.getId();
    }

    @Override
    public MicroPurchase getById(Long id) {
        return microPurchaseRepository.findById(id).orElse(null);
    }

    @Override
    public MicroPurchase update(MicroPurchase microPurchase) {
        return microPurchaseRepository.save(microPurchase);
    }

    @Override
    public Page<MicroPurchase> selectList(Integer pageNum, Integer pageSize, MicroPurchase microPurchase) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);

        return microPurchaseRepository.findAll(Example.of(microPurchase),pageable);
    }

    @Override
    public void deleteById(Long id) {
        microPurchaseRepository.deleteById(id);
    }

    @Override
    public List<MicroPurchase> listByStudentId(Long studentId) {
        MicroPurchase search = new MicroPurchase();
        search.setStudentId(studentId);
        return microPurchaseRepository.findAll(Example.of(search));
    }
}
