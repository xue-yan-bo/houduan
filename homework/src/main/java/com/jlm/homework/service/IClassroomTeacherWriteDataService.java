package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomTeacherWriteData;

import java.util.List;

public interface IClassroomTeacherWriteDataService {
    void save(ClassroomTeacherWriteData classroomTeacherWriteData);

    List<ClassroomTeacherWriteData> findByClassroomExercisesId(Long classroomExercisesId);
}
