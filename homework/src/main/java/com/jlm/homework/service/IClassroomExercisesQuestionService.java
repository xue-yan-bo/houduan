package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomExercisesQuestion;

import java.util.List;

public interface IClassroomExercisesQuestionService {
    void saveQuestionList(Long classroomExercisesId, List<ClassroomExercisesQuestion> questionList);

    List<ClassroomExercisesQuestion>  selectQuestionList(Long classroomExercisesId);
}
