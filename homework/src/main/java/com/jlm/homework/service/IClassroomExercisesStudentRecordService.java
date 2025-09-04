package com.jlm.homework.service;

import com.jlm.homework.dto.ExerciseWriteData;
import com.jlm.homework.entity.ClassroomExercisesStudentRecord;

import java.util.List;

public interface IClassroomExercisesStudentRecordService {
    List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId);

    ClassroomExercisesStudentRecord save(ClassroomExercisesStudentRecord studentRecord);

    List<ClassroomExercisesStudentRecord> selectByClassroomExercisesIdAndClass(Long classroomExercisesId, Long classId);

    void endAllAnswer(ExerciseWriteData exerciseWriteData);
}
