package com.jlm.homework.controller;


import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.StudentMicrolecture;
import com.jlm.homework.service.IMicrolectureService;
import com.jlm.homework.service.IStudentMicrolectureService;


import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "微课接口", description = "微课接口")
@RestController
@RequestMapping("/api/microlecture")
public class MicrolectureController {
    @Autowired
    private IMicrolectureService microlectureService;
    @Autowired
    private IStudentMicrolectureService studentMicrolectureService;


    @GetMapping("/page")
    @Operation(summary = "老师微课分页")
    public Page<Microlecture> page(@RequestParam(value = "schoolId", required = false) Long schoolId,
                                               @RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(value = "name", required = false) String name,
                                               @RequestParam(value = "teacherId", required = false) Long teacherId,
                                               @RequestParam(value = "teacherName", required = false) String teacherName,
                                               @RequestParam(value = "gradeId", required = false) Long gradeId,
                                               @RequestParam(value = "gradeName", required = false) String gradeName,
                                               @RequestParam(value = "classId", required = false) Long classId,
                                               @RequestParam(value = "className", required = false) String className,
                                               @RequestParam(value = "chapter", required = false) String chapter,
                                               @RequestParam(value = "subject", required = false) String subject,
                                               @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                               @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {



        // 查询分页结果
        Page<Microlecture> page = microlectureService.selectPage(pageNum,pageSize,schoolId,name,teacherId,teacherName,gradeId,gradeName,classId,className
                ,subject,chapter, startTime, endTime);
        return page;

    }


    @Operation(summary = "老师新增微课")
    @PostMapping("/save")
    public Microlecture save(@RequestBody Microlecture microlecture) {


            microlectureService.save(microlecture);
            studentMicrolectureService.createStudentMicrolecture(microlecture);

            return microlecture;

    }


    @GetMapping("/studentPage")
    @Operation(summary = "学生微课分页")
    public Page<StudentMicrolecture> studentPage(@RequestParam(value = "microlectureId", required = false) Long microlectureId,
                                                 @RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                 @RequestParam(value = "microlectureName", required = false) String microlecturename,
                                                 @RequestParam(value = "studentId", required = false) Long studentId,
                                                 @RequestParam(value = "studentName", required = false) String studentName,
                                                 @RequestParam(value = "status", required = false) Integer status,
                                                 @RequestParam(value = "searchType", defaultValue = "0") Integer searchType) {


        // 查询分页结果
        Page<StudentMicrolecture> page = studentMicrolectureService.page(pageNum,pageSize, microlectureId,microlecturename,studentId,studentName,status,searchType);
        return page;
    }



    @Operation(summary = "修改学生微课")
    @PostMapping("/update")
    public StudentMicrolecture update(@RequestBody StudentMicrolecture studentMicrolecture) {


        studentMicrolectureService.save(studentMicrolecture);
        return studentMicrolecture;

    }


    @Operation(summary = "学生开始微课")
    @GetMapping("/start")
    public StudentMicrolecture start(@RequestParam(value = "microlectureId", required = true) Long microlectureId,
                                                 @RequestParam(value = "studentId", required = true) Long studentId) {


        StudentMicrolecture studentMicrolecture=studentMicrolectureService.selectByMicrolectureAndStudent(microlectureId,studentId);
        if(studentMicrolecture!=null){
            studentMicrolecture.setStartTime(new Date());
            studentMicrolecture.setStatus(1);
            studentMicrolectureService.updateById(studentMicrolecture);

        }
        return studentMicrolecture;

    }
    @Operation(summary = "学生中间暂停或关闭视频微课")
    @GetMapping("/pauseOrClose")
    public StudentMicrolecture pause(@RequestParam(value = "microlectureId", required = false) Long microlectureId,
                                   @RequestParam(value = "studentId", required = false) Long studentId,
                                                 @RequestParam(value = "progressBar", required = false) Integer progressBar) {
        StudentMicrolecture studentMicrolecture=studentMicrolectureService.selectByMicrolectureAndStudent(microlectureId,studentId);
        if(studentMicrolecture!=null){
            studentMicrolecture.setProgressBar(progressBar);
            studentMicrolecture.setUpdateTime(new Date());
            studentMicrolectureService.updateById(studentMicrolecture);

        }
        return studentMicrolecture;

    }

    @Operation(summary = "学生结束微课")
    @GetMapping("/end")
    public StudentMicrolecture end(@RequestParam(value = "microlectureId", required = false) Long microlectureId,
                                                 @RequestParam(value = "studentId", required = false) Long studentId) {
        StudentMicrolecture studentMicrolecture=studentMicrolectureService.selectByMicrolectureAndStudent(microlectureId,studentId);
        if(studentMicrolecture!=null){
            studentMicrolecture.setEndTime(new Date());
            studentMicrolecture.setStatus(2);
            studentMicrolecture.setUpdateTime(new Date());
            studentMicrolecture.setProgressBar(100);
            studentMicrolectureService.updateById(studentMicrolecture);

        }
        return studentMicrolecture;

    }

    @GetMapping("/recordPage")
    @Operation(summary = "学生微课记录分页")
    public Page<StudentMicrolecture> recordPage(@RequestParam(value = "microlectureId", required = false) Long microlectureId,
                                                @RequestParam(value = "studentId", required = false) Long studentId,
                                                 @RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {


        // 查询分页结果
        Page<StudentMicrolecture> page = studentMicrolectureService.recordPage(pageNum,pageSize, microlectureId,studentId);
        return page;
    }
}
