package com.jlm.homework.controller;

import com.jlm.homework.dto.TeacherClassroomData;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.service.IClassroomExercisesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

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
        classroomExercises.setExercisesType(1);
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
     * @param
     * @return
     */
    @GetMapping("/queryList")
    public Page<ClassroomExercises> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize,
            Long classId,String homeworkName,String startTime,String endTime) {
        Integer exercisesType=1;
        Page<ClassroomExercises> list = classroomExercisesService.selectPurchaseList(pageNum,pageSize, classId,homeworkName,startTime,endTime,exercisesType);

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
    public Long teacherStartAnswer(Long classroomExercisesId,Long classId,Long schoolId, Integer exercisesType) throws Throwable {
        classroomExercisesId=classroomExercisesService.teacherStartAnswer(classroomExercisesId,classId,schoolId,exercisesType);
        return classroomExercisesId;
    }

    /**
     * 课堂数据
     */
    @GetMapping("/classroom-data")
    @Operation(summary = "课堂数据")
    public TeacherClassroomData getTeacherClassroomData(String startDate,String endDate) throws Throwable {
        TeacherClassroomData classroomData=classroomExercisesService.getTeacherClassroomData(startDate,endDate);
        return classroomData;
    }


    /**
     * 课堂互动分页查询
     * @param classId
     * @return
     */
    @GetMapping("/classInteractList")
    @Operation(summary = "课堂互动分页查询")
    public Page<ClassroomExercises> classInteractList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long classId,String homeworkName,String startTime,String endTime) {

        Integer exercisesType = 2;
        Page<ClassroomExercises> list = classroomExercisesService.classInteractList(pageNum,pageSize, classId,homeworkName,startTime,endTime,exercisesType);

        return list;
    }

    /**
     * 纸笔直播分页查询
     * @param classId
     * @return
     */
    @GetMapping("/liveStreamtList")
    @Operation(summary = "纸笔直播分页查询")
    public Page<ClassroomExercises> liveStreamtList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long classId,String homeworkName,String startTime,String endTime) {

        Integer exercisesType=3;
        Page<ClassroomExercises> list = classroomExercisesService.classInteractList(pageNum,pageSize, classId,homeworkName,startTime,endTime,exercisesType);
        return list;
    }

    /**
     * 分页查询随堂检测、课堂互动、纸笔直播
     * @param
     * @return
     */
    @GetMapping("/selectAllList")
    @Operation(summary = "分页查询随堂检测、课堂互动、纸笔直播",description = "exercisesType 1随堂检测、2课堂互动、3纸笔直播")
    public Page<ClassroomExercises> selectAllList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long classId,Integer exercisesType,String homeworkName,String startTime,String endTime) {

        Page<ClassroomExercises> list = classroomExercisesService.classInteractList(pageNum,pageSize, classId,homeworkName,startTime,endTime,exercisesType);

        return list;
    }
}
