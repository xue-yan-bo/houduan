package com.jlm.homework.controller;

import com.jlm.homework.entity.ChapterSection;
import com.jlm.homework.service.IChapterSectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "教材章节", description = "章节的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/chapter-section")
public class ChapterSectionController {
    @Autowired
    private IChapterSectionService chapterSectionService;
    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody ChapterSection chapterSection) throws Throwable {
        Long id=chapterSectionService.create(chapterSection);
        return id;
    }

    @GetMapping("/{id}")
    public ChapterSection getById(@PathVariable Long id) {
        ChapterSection homeworkPublish = chapterSectionService.getById(id);
        return homeworkPublish;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public ChapterSection update(@RequestBody ChapterSection chapterSection) {
        chapterSection=chapterSectionService.update(chapterSection);
        return chapterSection;


    }

    /**
     * 分页查询
     * @param chapterSection
     * @return
     */
    @GetMapping("/queryList")
    public Page<ChapterSection> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            ChapterSection chapterSection) {

        Page<ChapterSection> list = chapterSectionService.selectList(pageNum,pageSize, chapterSection);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        chapterSectionService.deleteById(id);

    }
}
