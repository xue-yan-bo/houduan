package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomExercises;
import org.springframework.data.domain.Page;

public interface IClassroomExercisesService {
    Long create(ClassroomExercises classroomExercises);

    ClassroomExercises getById(Long id);

    ClassroomExercises update(ClassroomExercises classroomExercises);

    Page<ClassroomExercises> selectList(Integer pageNum, Integer pageSize, ClassroomExercises classroomExercises);

    void deleteById(Long id);

    ClassroomExercises publish(ClassroomExercises classroomExercises);
}
