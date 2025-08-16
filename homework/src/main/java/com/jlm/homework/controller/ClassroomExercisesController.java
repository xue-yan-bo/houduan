package com.jlm.homework.controller;

import com.jlm.homework.dto.HomeworkPublishRequest;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesStudentRecord;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.service.IClassroomExercisesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "随堂检测", description = "随堂检测发布的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/classroom-exercises")
public class ClassroomExercisesController {
    @Autowired
    private IClassroomExercisesService classroomExercisesService;
    /**
     * 创建
     */
    @PostMapping("/create")
    public Long create(@RequestBody ClassroomExercises classroomExercises) throws Throwable {
        classroomExercises.setPublishStatus(0);
        Long id=classroomExercisesService.create(classroomExercises);
        return id;
    }

    @GetMapping("/{id}")
    public ClassroomExercises getById(@PathVariable Long id) {
        ClassroomExercises classroomExercises = classroomExercisesService.getById(id);

        return classroomExercises;
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public ClassroomExercises update(@RequestBody ClassroomExercises classroomExercises) {
        classroomExercises=classroomExercisesService.update(classroomExercises);
        return classroomExercises;


    }
    /**
     * 发布
     */
    @PostMapping("/publish")
    @Operation(summary = "发布练习",description = "随堂检测中的发布练习接口")
    public ClassroomExercises publish(@RequestBody ClassroomExercises classroomExercises) {
        classroomExercises=classroomExercisesService.publish(classroomExercises);
        return classroomExercises;


    }


    /**
     * 分页查询
     * @param classroomExercises
     * @return
     */
    @GetMapping("/queryList")
    public Page<ClassroomExercises> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            ClassroomExercises classroomExercises) {

        Page<ClassroomExercises> list = classroomExercisesService.selectList(pageNum,pageSize, classroomExercises);

        return list;
    }

    @GetMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        classroomExercisesService.deleteById(id);

    }

    /**
     * 老师点击开始答题按钮
     */
    @GetMapping("/teacher-startAnswer")
    @Operation(summary = "老师点击开始答题按钮")
    public void teacherStartAnswer(Long classroomExercisesId,Long classId) throws Throwable {
        classroomExercisesService.teacherStartAnswer(classroomExercisesId,classId);
    }
}
