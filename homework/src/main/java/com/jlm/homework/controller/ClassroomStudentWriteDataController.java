package com.jlm.homework.controller;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.ClassroomTeacherWriteData;
import com.jlm.homework.entity.ClassroomTearcherApproveStu;
import com.jlm.homework.service.IClassroomStudentWriteDataService;
import com.jlm.homework.service.IClassroomTeacherWriteDataService;
import com.jlm.homework.service.IClassroomTearcherApproveStuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 课堂学生手写控制器
 * 提供课堂练习学生答题记录相关的REST接口
 */
@Tag(name = "课堂学生手写智能板", description = "课堂学生手写智能板的确认、下一页等")
@RestController
@RequestMapping("/api/classroom-student-write")
public class ClassroomStudentWriteDataController {
    @Autowired
    private IClassroomStudentWriteDataService classroomStudentWriteDataService;
    @Autowired
    private IClassroomTeacherWriteDataService classroomTeacherWriteDataService;
    @Autowired
    private IClassroomTearcherApproveStuService classroomTearcherApproveStuService;
    @PostMapping("/next")
    public void confirm(@RequestBody ClassroomStudentWriteData studentWriteData){
        classroomStudentWriteDataService.save(studentWriteData);
    }
    @PostMapping("/teacherNext")
    @Operation(summary = "老师书写下一页或中间保存")
    public void teacherNext(@RequestBody ClassroomTeacherWriteData teacherWriteData){
        classroomTeacherWriteDataService.save(teacherWriteData);
    }

    @PostMapping("/teacherApproveNext")
    @Operation(summary = "课堂老师批阅下一页或中间保存")
    public void teacherNext(@RequestBody ClassroomTearcherApproveStu tearcherApproveStu){
        classroomTearcherApproveStuService.save(tearcherApproveStu);
    }

    @GetMapping("/clearTeacherApprove")
    @Operation(summary = "清除课堂老师批阅")
    public void clearTeacherApprove(Long classroomExercisesId,Long studentId,Integer pageNum){
        classroomTearcherApproveStuService.clearTeacherApprove(classroomExercisesId,studentId,pageNum);
    }

    @GetMapping("/clearTeacher2Approve")
    @Operation(summary = "清除课堂老师对老师的批阅")
    public void clearTeacher2Approve(Long classroomExercisesId,Long teacherId,Integer pageNum){
        classroomTearcherApproveStuService.clearTeacher2Approve(classroomExercisesId,teacherId,pageNum);
    }
}
