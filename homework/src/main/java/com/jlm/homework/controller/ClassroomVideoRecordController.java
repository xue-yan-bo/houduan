package com.jlm.homework.controller;

import com.jlm.homework.entity.ClassroomVideoRecord;
import com.jlm.homework.service.IClassroomVideoRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课堂录制", description = "课堂录制相关接口等")
@RestController
@RequestMapping("/api/classroom-video-record")
public class ClassroomVideoRecordController {
    @Autowired
    private IClassroomVideoRecordService classroomVideoRecordService;
    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody ClassroomVideoRecord classroomVideoRecord) throws Throwable {
        Long id=classroomVideoRecordService.create(classroomVideoRecord);
        return id;
    }

    @GetMapping("/{id}")
    public ClassroomVideoRecord getById(@PathVariable Long id) {
        ClassroomVideoRecord classroomVideoRecord = classroomVideoRecordService.getById(id);
        return classroomVideoRecord;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public ClassroomVideoRecord update(@RequestBody ClassroomVideoRecord classroomVideoRecord) {
        classroomVideoRecord=classroomVideoRecordService.update(classroomVideoRecord);
        return classroomVideoRecord;


    }

    /**
     * 分页查询
     * @param classroomVideoRecord
     * @return
     */
    @GetMapping("/queryList")
    public Page<ClassroomVideoRecord> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            ClassroomVideoRecord classroomVideoRecord) {

        Page<ClassroomVideoRecord> list = classroomVideoRecordService.selectList(pageNum,pageSize, classroomVideoRecord);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        classroomVideoRecordService.deleteById(id);

    }
}
