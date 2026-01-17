package com.jlm.homework.controller;

import com.jlm.homework.entity.Copybook;
import com.jlm.homework.service.ICopybookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "字帖", description = "字帖相关接口等")
@RestController
@RequestMapping("/api/copybook")
public class CopybookController {
    @Autowired
    private ICopybookService copybookService;

    /**
     * 创建
     */
    @PostMapping("/create")
    @Operation(summary = "创建")
    public Long create(@RequestBody Copybook copybook) throws Throwable {
        Long id=copybookService.create(copybook);
        return id;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询详情")
    public Copybook getById(@PathVariable Long id) {
        Copybook copybook = copybookService.getById(id);
        return copybook;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    @Operation(summary = "编辑")
    public Copybook update(@RequestBody Copybook copybook) {
        copybook=copybookService.update(copybook);
        return copybook;


    }

    /**
     * 分页查询
     * @param copybook
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "分页查询")
    public Page<Copybook> queryList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Copybook copybook) {

        Page<Copybook> list = copybookService.selectList(pageNum,pageSize, copybook);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除")
    public void deleteById(@PathVariable Long id) {
        copybookService.deleteById(id);

    }

    @GetMapping("/publish")
    @Operation(summary = "发布")
    public Boolean publish(Long id) {
        Boolean publishFlag = copybookService.publish(id);
        return publishFlag;
    }
}
