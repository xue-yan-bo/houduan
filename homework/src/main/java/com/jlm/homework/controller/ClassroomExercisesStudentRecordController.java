package com.jlm.homework.controller;

import com.jlm.homework.dto.ExerciseWriteData;
import com.jlm.homework.dto.StudentWriteDto;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesStudentRecord;
import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.service.IClassroomExercisesStudentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 课堂练习学生答题记录控制器
 * 提供课堂练习学生答题记录相关的REST接口
 */
@Tag(name = "课堂练习学生答题记录", description = "课堂练习学生答题记录的开始答题、结束答题等")
@RestController
@RequestMapping("/api/classroom-exercises-student-record")
public class ClassroomExercisesStudentRecordController {
    @Autowired
    private IClassroomExercisesStudentRecordService classroomExercisesStudentRecordService;
    /**
     * 开始答题
     */
    @PostMapping("/startAnswer")
    @Operation(summary = "开始答题")
    public ClassroomExercisesStudentRecord startAnswer(@RequestBody ClassroomExercisesStudentRecord studentRecord) throws Throwable {
        if(studentRecord.getStudentId()==null){
            throw new RuntimeException("学生ID不能为空！");
        }
        studentRecord.setStartFlag(1);
        studentRecord.setStartTime(new Date());
        studentRecord.setEndFlag(0);
        studentRecord=classroomExercisesStudentRecordService.save(studentRecord);
        return studentRecord;
    }

    /**
     * 结束答题
     */
    @PostMapping("/endAnswer")
    @Operation(summary = "结束答题")
    public ClassroomExercisesStudentRecord endAnswer(@RequestBody ClassroomExercisesStudentRecord studentRecord) throws Throwable {
        if(studentRecord.getStudentId()==null){
            throw new RuntimeException("学生ID不能为空！");
        }
        studentRecord.setEndFlag(1);
        Date now = new Date();
        studentRecord.setEndTime(now);
        if(studentRecord.getStartTime()!=null){
            studentRecord.setAnswerDuration(now.getTime() - studentRecord.getStartTime().getTime());
        }
        studentRecord=classroomExercisesStudentRecordService.save(studentRecord);
        return studentRecord;
    }
    /**
     * 获取学生答题记录
     */
    @GetMapping("/studentRecord")
    @Operation(summary = "获取学生答题记录")
    public List<ClassroomExercisesStudentRecord> getClassroomExercisesStudentRecords(Long classroomExercisesId,Long classId ){
        List<ClassroomExercisesStudentRecord> recordList= classroomExercisesStudentRecordService.selectByClassroomExercisesIdAndClass(classroomExercisesId,classId);
        return recordList;
    }

    /**
     * 老师结束所有答题
     */
    @PostMapping("/end-all-answer")
    @Operation(summary = "老师结束所有答题")
    public void endAllAnswer(@RequestBody ExerciseWriteData exerciseWriteData ){
        classroomExercisesStudentRecordService.endAllAnswer(exerciseWriteData);

    }
    /**
     * 获取课堂互动记录
     */
    @GetMapping("/classInteractRecord")
    @Operation(summary = "获取课堂互动记录")
    public List<ClassroomExercisesStudentRecord> getClassInteractRecord(Long classroomExercisesId,Long classId ){
        List<ClassroomExercisesStudentRecord> recordList= classroomExercisesStudentRecordService.getClassInteractRecord(classroomExercisesId,classId);
        return recordList;
    }
    /**
     * 获取学生直播记录
     */
    @GetMapping("/liveStreamtRecord")
    @Operation(summary = "获取学生直播记录")
    public List<StudentWriteDto> getLiveStreamtRecord(Long classroomExercisesId,Long classId ){
        List<StudentWriteDto> recordList= classroomExercisesStudentRecordService.getLiveStreamtRecord(classroomExercisesId,classId);
        return recordList;
    }
}
