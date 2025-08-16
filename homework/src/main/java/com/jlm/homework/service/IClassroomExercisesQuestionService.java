package com.jlm.homework.service;

import com.jlm.homework.dto.ExerciseTypeAnalyse;
import com.jlm.homework.dto.TitleVolumeAnalyse;
import com.jlm.homework.entity.ClassroomExercisesQuestion;

import java.util.List;

public interface IClassroomExercisesQuestionService {
    void saveQuestionList(Long classroomExercisesId, List<ClassroomExercisesQuestion> questionList);

    List<ClassroomExercisesQuestion>  selectQuestionList(Long classroomExercisesId);

    ClassroomExercisesQuestion save(Long classroomExercisesId, ClassroomExercisesQuestion question);

    void delete(Long id);

    ClassroomExercisesQuestion findById(Long id);

    TitleVolumeAnalyse titleVolumeAnalyse(String subject, Long classId, String startDate, String endDate);

    List<ClassroomExercisesQuestion> findQuestionList(Long classId, String startDate, String endDate);
}
