package com.jlm.homework.service;

import com.jlm.homework.dto.ClassroomExercisesStudentStatistics;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;

import java.util.List;

public interface IClassroomExercisesStudentAnswerService {
    ClassroomExercisesStudentAnswer save(ClassroomExercisesStudentAnswer studentAnswer);
    ClassroomExercisesStudentAnswer findById(Long id);
    List<ClassroomExercisesStudentAnswer> findByClassroomExercisesId(Long classroomExercisesId);

    ClassroomExercisesStudentStatistics statisticsByClassroomExercisesId(Long classroomExercisesId);
}
