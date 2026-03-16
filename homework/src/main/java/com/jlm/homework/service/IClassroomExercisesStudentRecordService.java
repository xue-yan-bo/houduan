package com.jlm.homework.service;

import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.homework.dto.ExerciseWriteData;
import com.jlm.homework.dto.StudentWriteDto;
import com.jlm.homework.dto.TeacherWriteDto;
import com.jlm.homework.entity.ClassroomExercisesStudentRecord;

import java.util.List;

public interface IClassroomExercisesStudentRecordService {
    List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId);

    ClassroomExercisesStudentRecord save(ClassroomExercisesStudentRecord studentRecord);

    List<ClassroomExercisesStudentRecord> selectByClassroomExercisesIdAndClass(Long classroomExercisesId, Long classId);

    List<ClassroomExercisesStudentRecord> studentRecordNoWriteData(Long classroomExercisesId, Long classId);

    void endAllAnswer(ExerciseWriteData exerciseWriteData);

    List<StudentWriteDto> getLiveStreamtRecord(Long classroomExercisesId, Long classId);

    List<ClassroomExercisesStudentRecord> getClassInteractRecord(Long classroomExercisesId, Long classId);

    void aiParseWriteRecord(Long studentRecordId);

    /**
     * AI 结构化
     * @param studentRecordId
     */
    void aiParseWriteStrucRecord(Long studentRecordId);

    TeacherWriteDto getLiveStreamtRecordTeacher(Long classroomExercisesId);

    ClassroomExercisesStudentRecord getStudentRecordDetail(Long studentRecordId);
    /**
     * AI中台
     * @param studentRecordId
     */
    void aiParseWriteMid(Long studentRecordId);

    void aiResultDeal(Long studentRecordId, List<SubQuestionsEnt> answers);
}
