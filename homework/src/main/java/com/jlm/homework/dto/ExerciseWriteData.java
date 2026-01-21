package com.jlm.homework.dto;

import com.jlm.homework.entity.ClassroomTearcherApproveStu;
import com.jlm.homework.entity.TeacherWriteRecord;
import lombok.Data;

import java.util.List;
@Data
public class ExerciseWriteData {
    /**
     * 课堂练习ID
     */
    private Long classroomExercisesId;
    /**
     * 班级ID
     */
    private Long classId;
    /**
     * 学生书写列表
     */
    private List<StudentWriteDto> studentWriteList;
    /**
     * 老师黑板出题轨迹
     */
    private List<TeacherWriteRecord> teacherWriteRecords;

    /**
     * 老师审批学生作答笔记
     */
    private List<ClassroomTearcherApproveStu> tearcherApproveStuList;

    /**
     * 老师板子书写笔记
     */
    private TeacherWriteDto TeacherWriteDto;
}
