package com.jlm.homework.controller;

import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.service.IHomeworkPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/homeworkPublish")
public class HomeworkPublishController {
    @Autowired
    private IHomeworkPublishService homeworkPublishService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public ResultDto<String> create(HomeworkPublish homeworkPublish) {
        ResultDto<String> resultDto = new ResultDto<>();
        String id=homeworkPublishService.create(homeworkPublish);
        resultDto.setData(id);
        return resultDto;
    }

    @GetMapping("/{id}")
    public ResultDto<HomeworkPublish> getById(@PathVariable Long id) {
        ResultDto<HomeworkPublish> resultDto = new ResultDto<>();
        HomeworkPublish homeworkPublish = homeworkPublishService.getById(id);
        resultDto.setSuccess(true);
        resultDto.setData(homeworkPublish);
        return resultDto;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public ResultDto<HomeworkPublish> update(HomeworkPublish homeworkPublish) {
        ResultDto<HomeworkPublish> resultDto = new ResultDto<>();
        homeworkPublish=homeworkPublishService.update(homeworkPublish);
        resultDto.setData(homeworkPublish);
        return resultDto;


    }

    /**
     * 分页查询
     * @param page
     * @param homeworkPublish
     * @return
     */
    @GetMapping("/queryList")
    public ResultDto<Page<HomeworkPublish>> selectPurchaseList(Page<HomeworkPublish> page, HomeworkPublish homeworkPublish) {
        ResultDto<Page<HomeworkPublish>> resultDto = new ResultDto<>();
        Page<HomeworkPublish> list = homeworkPublishService.selectList(page, homeworkPublish);
        resultDto.setSuccess(true);
        resultDto.setData(list);
        return resultDto;
    }
}
