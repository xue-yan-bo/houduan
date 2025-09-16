package com.jlm.homework.controller;

import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.service.IQuestionBankService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "题库", description = "题库的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/questionBank")
public class QuestionBankController {
    @Autowired
    private IQuestionBankService questionBankService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody QuestionBank questionBank) throws Throwable {
        Long id=questionBankService.create(questionBank);
        return id;
    }

    @GetMapping("/{id}")
    public QuestionBank getById(@PathVariable Long id) {
        QuestionBank homeworkPublish = questionBankService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public QuestionBank update(@RequestBody QuestionBank questionBank) {
        questionBank=questionBankService.update(questionBank);
        return questionBank;


    }

    /**
     * 分页查询
     * @param questionBank
     * @return
     */
    @GetMapping("/queryList")
    public Page<QuestionBank> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            QuestionBank questionBank) {

        Page<QuestionBank> list = questionBankService.selectList(pageNum,pageSize, questionBank);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        questionBankService.deleteById(id);

    }
}
