package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.MicroPurchase;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.repository.MicroPurchaseRepository;
import com.jlm.homework.service.IMicroPurchaseService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class MicroPurchaseServiceImpl implements IMicroPurchaseService {
    @Resource
    private MicroPurchaseRepository microPurchaseRepository;
    @Autowired
    private UserService userService;
    @Override
    public Long create(MicroPurchase microPurchase) {
        microPurchase.setCreateTime(new Date());
        if(microPurchase.getSchoolId()==null){
            microPurchase.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        microPurchase=microPurchaseRepository.save(microPurchase);
        return microPurchase.getId();
    }

    @Override
    public MicroPurchase getById(Long id) {
        return microPurchaseRepository.findById(id).orElse(null);
    }

    @Override
    public MicroPurchase update(MicroPurchase microPurchase) {
        if(microPurchase.getSchoolId()==null){
            microPurchase.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        return microPurchaseRepository.save(microPurchase);
    }

    @Override
    public Page<MicroPurchase> selectList(Integer pageNum, Integer pageSize, MicroPurchase microPurchase) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        Specification<MicroPurchase> specification = new Specification<MicroPurchase>() {
            @Override
            public Predicate toPredicate(Root<MicroPurchase> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if (microPurchase.getMicroGradeId() != null) {
                        Predicate con = criteriaBuilder.equal(root.get("microGradeId"),microPurchase.getMicroGradeId());
                        list.add(con);
                    }
                    if(StringUtils.isNotEmpty(microPurchase.getMicroGradeName())){
                        Predicate con1 = criteriaBuilder.like(root.get("microGradeName").as(String.class),"%"+microPurchase.getMicroGradeName()+"%");
                        list.add(con1);
                    }
                    if (microPurchase.getStudentId() != null) {
                        Predicate con = criteriaBuilder.equal(root.get("studentId"),microPurchase.getStudentId());
                        list.add(con);
                    }
                    if(StringUtils.isNotEmpty(microPurchase.getStudentName())){
                        Predicate con1 = criteriaBuilder.like(root.get("studentName").as(String.class),"%"+microPurchase.getStudentName()+"%");
                        list.add(con1);
                    }
                    if(StringUtils.isNotEmpty(microPurchase.getSubject())){
                        Predicate con1 = criteriaBuilder.like(root.get("subject").as(String.class),"%"+microPurchase.getSubject()+"%");
                        list.add(con1);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return microPurchaseRepository.findAll(specification,pageable);
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
