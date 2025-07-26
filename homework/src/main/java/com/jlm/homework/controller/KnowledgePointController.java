package com.jlm.homework.controller;

import com.jlm.homework.entity.KnowledgePoint;
import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.service.IKnowledgePointService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "知识点", description = "知识点的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/knowledge-point")
public class KnowledgePointController {
    @Autowired
    private IKnowledgePointService knowledgePointService;
    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody KnowledgePoint knowledgePoint) throws Throwable {
        Long id=knowledgePointService.create(knowledgePoint);
        return id;
    }

    @GetMapping("/{id}")
    public KnowledgePoint getById(@PathVariable Long id) {
        KnowledgePoint homeworkPublish = knowledgePointService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public KnowledgePoint update(@RequestBody KnowledgePoint knowledgePoint) {
        knowledgePoint=knowledgePointService.update(knowledgePoint);
        return knowledgePoint;


    }

    /**
     * 分页查询
     * @param knowledgePoint
     * @return
     */
    @GetMapping("/queryList")
    public Page<KnowledgePoint> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            KnowledgePoint knowledgePoint) {

        Page<KnowledgePoint> list = knowledgePointService.selectList(pageNum,pageSize, knowledgePoint);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        knowledgePointService.deleteById(id);

    }
}
