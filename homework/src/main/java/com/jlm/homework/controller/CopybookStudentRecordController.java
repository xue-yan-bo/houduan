package com.jlm.homework.controller;

import com.jlm.homework.entity.CopybookStudentRecord;
import com.jlm.homework.service.ICopybookStudentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "学生书写字帖记录", description = "学生书写字帖相关接口等")
@RestController
@RequestMapping("/api/copybook-record")
public class CopybookStudentRecordController {
    @Autowired
    private ICopybookStudentRecordService copybookStudentRecordService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询详情")
    public CopybookStudentRecord getById(@PathVariable Long id) {
        CopybookStudentRecord record = copybookStudentRecordService.getById(id);
        return record;
    }

    /**
     * 分页查询
     * @param copybook
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "分页查询")
    public Page<CopybookStudentRecord> queryList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            CopybookStudentRecord copybook) {

        Page<CopybookStudentRecord> list = copybookStudentRecordService.selectList(pageNum,pageSize, copybook);

        return list;
    }
}
