package com.jlm.homework.controller;

import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.exception.BusinessException;
import com.jlm.homework.exception.ParameterException;
import com.jlm.homework.service.IHomeworkPublishService;
import com.jlm.homework.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/homeworkPublish")
public class HomeworkPublishController {
    @Autowired
    private IHomeworkPublishService homeworkPublishService;
    @Autowired
    private UserService userService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public String create(@RequestBody HomeworkPublish homeworkPublish) throws Throwable {
        ResultDto<String> resultDto = new ResultDto<>();
        homeworkPublish.setUserId(userService.getCurrentUserId());
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
     * @param homeworkPublish
     * @return
     */
    @GetMapping("/queryList")
    public Page<HomeworkPublish> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HomeworkPublish homeworkPublish) {

        Page<HomeworkPublish> list = homeworkPublishService.selectList(pageNum,pageSize, homeworkPublish);

        return list;
    }

    @GetMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        homeworkPublishService.deleteById(id);

    }
}
