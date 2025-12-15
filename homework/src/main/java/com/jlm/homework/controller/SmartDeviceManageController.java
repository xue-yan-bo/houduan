package com.jlm.homework.controller;

import com.jlm.homework.dto.StudentVo;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.Student;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 智能设备管理
 */
@Tag(name = "智能设备管理", description = "智能设备管理的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/smart-device-manage")
public class SmartDeviceManageController {
    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;

    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody SmartDeviceUserRelation deviceUserRelation) throws Throwable {
        Long id=smartDeviceUserRelationService.create(deviceUserRelation);
        return id;
    }

    @GetMapping("/{id}")
    public SmartDeviceUserRelation getById(@PathVariable Long id) {
        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.getById(id);
        return deviceUserRelation;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public SmartDeviceUserRelation update(@RequestBody SmartDeviceUserRelation deviceUserRelation) {
        deviceUserRelation=smartDeviceUserRelationService.update(deviceUserRelation);
        return deviceUserRelation;
    }

    /**
     * 分页查询
     * @param student
     * @return
     */
    @GetMapping("/queryList")
    @Operation(summary = "学生手写板绑定")
    public Page<StudentVo> queryList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Student student) {

        Page<StudentVo> list = smartDeviceUserRelationService.selectList(pageNum,pageSize, student);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        smartDeviceUserRelationService.deleteById(id);

    }

    /**
     * 分页查询
     * @param student
     * @return
     */
    @GetMapping("/wxBindList")
    @Operation(summary = "学生手写板绑定")
    public Page<StudentVo> wxBindList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Student student) {

        Page<StudentVo> list = smartDeviceUserRelationService.wxBindList(pageNum,pageSize, student);

        return list;
    }

    @GetMapping("/getByUserId")
    public SmartDeviceUserRelation getByUseId(String userId) {
        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.getByUseId(userId);
        return deviceUserRelation;
    }
}
