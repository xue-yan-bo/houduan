package com.jlm.homework.controller;

import com.jlm.homework.dto.ClassroomExercisesStudentStatistics;
import com.jlm.homework.dto.KnowledgePointAnalysis;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.service.IClassroomExercisesStudentAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课堂练习学生作业统计控制器
 * 提供课堂练习学生作业统计相关的REST接口
 */
@Tag(name = "课堂练习学生作业统计", description = "课堂练习学生作业统计")
@RestController
@RequestMapping("/api/classroom-exercises-student-answer")
public class ClassroomExercisesStudentAnswerController {
    @Autowired
    private IClassroomExercisesStudentAnswerService classroomExercisesStudentAnswerService;

    @PostMapping("/update")
    @Operation(summary = "学生提交答案")
    public ClassroomExercisesStudentAnswer update(ClassroomExercisesStudentAnswer studentAnswer){
        return classroomExercisesStudentAnswerService.save(studentAnswer);
    }
    /**
     * 统计分析
     */
    @GetMapping("/statistics-analysis")
    @Operation(summary = "随堂检测的统计分析",description = "随堂检测的统计分析")
    public ClassroomExercisesStudentStatistics getExercisesStudentStatistics(Long classroomExercisesId) {
        ClassroomExercisesStudentStatistics statistics = new ClassroomExercisesStudentStatistics();
        statistics = classroomExercisesStudentAnswerService.statisticsByClassroomExercisesId(classroomExercisesId);

        return statistics;
    }
    /**
     * 统计分析
     */
    @GetMapping("/knowledgePoint-analysis")
    @Operation(summary = "知识点分析",description = "知识点分析")
    public KnowledgePointAnalysis getKnowledgePointAnalysis(String subject, Long classId, String startDate, String endDate) {
        return classroomExercisesStudentAnswerService.getKnowledgePointAnalysis(subject,classId,startDate,endDate);
    }
}
