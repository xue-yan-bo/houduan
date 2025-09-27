package com.jlm.homework.controller;

import com.jlm.homework.dto.HomeworkPublishRequest;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.service.IHomeworkPublishService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "作业发布", description = "作业发布的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/homework-publish")
public class HomeworkPublishController {
    @Autowired
    private IHomeworkPublishService homeworkPublishService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public String create(@RequestBody HomeworkPublish homeworkPublish) throws Throwable {
        String id=homeworkPublishService.create(homeworkPublish);
        return id;
    }

    @GetMapping("/{id}")
    public HomeworkPublish getById(@PathVariable Long id) {
        HomeworkPublish homeworkPublish = homeworkPublishService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public HomeworkPublish update(@RequestBody HomeworkPublish homeworkPublish) {
        homeworkPublish=homeworkPublishService.update(homeworkPublish);
        return homeworkPublish;


    }

    /**
     * 分页查询
     * @param homeworkPublishRequest
     * @return
     */
    @GetMapping("/queryList")
    public Page<HomeworkPublish> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HomeworkPublishRequest homeworkPublishRequest) {

        Page<HomeworkPublish> list = homeworkPublishService.selectList(pageNum,pageSize, homeworkPublishRequest);

        return list;
    }

    @GetMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        homeworkPublishService.deleteById(id);
    }
}
