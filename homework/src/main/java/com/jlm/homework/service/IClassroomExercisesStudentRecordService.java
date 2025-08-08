package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomExercisesStudentRecord;

import java.util.List;

public interface IClassroomExercisesStudentRecordService {
    List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId);
}
