package com.jlm.homework.controller;

import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
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
    public void groupPapers(@PathVariable Long classroomExercisesId, @RequestBody List<ClassroomExercisesQuestion> questionList) throws Throwable {
        classroomExercisesQuestionService.saveQuestionList(classroomExercisesId,questionList);
    }


}
