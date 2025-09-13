package com.jlm.homework.service;

import com.jlm.homework.dto.ClassroomExercisesData;
import com.jlm.homework.dto.ExerciseTypeAnalyse;
import com.jlm.homework.dto.TeacherClassroomData;
import com.jlm.homework.entity.ClassroomExercises;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IClassroomExercisesService {
    Long create(ClassroomExercises classroomExercises);

    ClassroomExercises getById(Long id);

    ClassroomExercises update(ClassroomExercises classroomExercises);

    Page<ClassroomExercises> selectList(Integer pageNum, Integer pageSize, ClassroomExercises classroomExercises);

    void deleteById(Long id);

    ClassroomExercises publish(ClassroomExercises classroomExercises);

    ExerciseTypeAnalyse exerciseTypeAnalyse(Long classId, String startDate, String endDate);

    void teacherStartAnswer(Long classroomExercisesId, Long classId,Long schoolId,Integer exercisesType);

    TeacherClassroomData getTeacherClassroomData(String startDate, String endDate);

}
