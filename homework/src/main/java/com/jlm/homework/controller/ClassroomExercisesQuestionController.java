package com.jlm.homework.controller;

import com.jlm.homework.dto.ExerciseTypeAnalyse;
import com.jlm.homework.dto.TitleVolumeAnalyse;
import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "随堂检测组卷", description = "随堂检测组卷的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/classroom-exercises-question")
public class ClassroomExercisesQuestionController {
    @Autowired
    private IClassroomExercisesQuestionService classroomExercisesQuestionService;

    /**
     * 组卷
     */
    @PostMapping("/groupPapers/{classroomExercisesId}")
    public void groupPapers(@PathVariable Long classroomExercisesId, @RequestBody List<ClassroomExercisesQuestion> questionList){
        classroomExercisesQuestionService.saveQuestionList(classroomExercisesId,questionList);
    }
    /**
     * 增改课堂检测题
     */
    @GetMapping("/{id}")
    public ClassroomExercisesQuestion findById(@PathVariable Long id) {
        return  classroomExercisesQuestionService.findById(id);
    }
    /**
     * 增改课堂检测题
     */
    @PostMapping("/save/{classroomExercisesId}")
    public ClassroomExercisesQuestion save(@PathVariable Long classroomExercisesId, @RequestBody ClassroomExercisesQuestion question) {
        ClassroomExercisesQuestion exercisesQuestion=classroomExercisesQuestionService.save(classroomExercisesId,question);
        return exercisesQuestion;
    }

    /**
     * 增改课堂检测题
     */
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        classroomExercisesQuestionService.delete(id);
    }

    /**
     * 题量分析
     */
    @GetMapping("/titleVolume-analyse")
    @Operation(summary = "题量分析" ,description = "课堂练习的题量分析")
    public TitleVolumeAnalyse titleVolumeAnalyse(String subject,Long classId,String startDate,String endDate) {
        return  classroomExercisesQuestionService.titleVolumeAnalyse(subject,classId,startDate,endDate);
    }

    /**
     * 练习类型分析
     */
    @GetMapping("/type-analyse")
    @Operation(summary = "练习类型分析" ,description = "课堂练习的练习类型分析")
    public ExerciseTypeAnalyse exerciseTypeAnalyse(Long classId, String startDate, String endDate) {
        return  classroomExercisesQuestionService.exerciseTypeAnalyse(classId,startDate,endDate);
    }
}
