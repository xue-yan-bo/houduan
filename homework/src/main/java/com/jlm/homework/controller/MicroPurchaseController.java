package com.jlm.homework.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.MicroPurchase;
import com.jlm.homework.service.IMicroPurchaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Tag(name = "微课购买接口", description = "微课购买接口")
@RestController
@RequestMapping("/api/micro-purchase")
public class MicroPurchaseController {
    @Autowired
    private IMicroPurchaseService microPurchaseService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody MicroPurchase microPurchase) throws Throwable {
        Long id=microPurchaseService.create(microPurchase);
        return id;
    }

    @GetMapping("/{id}")
    public MicroPurchase getById(@PathVariable Long id) {
        MicroPurchase homeworkPublish = microPurchaseService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public MicroPurchase update(@RequestBody MicroPurchase microPurchase) {
        microPurchase=microPurchaseService.update(microPurchase);
        return microPurchase;


    }

    /**
     * 分页查询
     * @param microPurchase
     * @return
     */
    @GetMapping("/queryList")
    public Page<MicroPurchase> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            MicroPurchase microPurchase) {

        Page<MicroPurchase> list = microPurchaseService.selectList(pageNum,pageSize, microPurchase);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        microPurchaseService.deleteById(id);

    }
}
