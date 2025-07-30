package com.jlm.homework.controller;

import com.jlm.homework.entity.QuestionType;
import com.jlm.homework.service.IQuestionTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "题型", description = "题型的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/questionType")
public class QuestionTypeController {
    @Autowired
    private IQuestionTypeService questionTypeService;
    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody QuestionType questionType) throws Throwable {
        Long id=questionTypeService.create(questionType);
        return id;
    }

    @GetMapping("/{id}")
    public QuestionType getById(@PathVariable Long id) {
        QuestionType homeworkPublish = questionTypeService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public QuestionType update(@RequestBody QuestionType questionType) {
        questionType=questionTypeService.update(questionType);
        return questionType;


    }

    /**
     * 分页查询
     * @param QuestionType
     * @return
     */
    @GetMapping("/queryList")
    public Page<QuestionType> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            QuestionType questionType) {

        Page<QuestionType> list = questionTypeService.selectList(pageNum,pageSize, questionType);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        questionTypeService.deleteById(id);

    }
}
