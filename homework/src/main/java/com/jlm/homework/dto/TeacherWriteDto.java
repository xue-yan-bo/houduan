package com.jlm.homework.dto;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.ClassroomTeacherWriteData;
import lombok.Data;

import java.util.List;
@Data
public class TeacherWriteDto {
    /**
     * 老师ID
     */
    private Long teacherId;

    private String teacherName;
    /**
     * 老师书写笔记
     */
    private List<ClassroomTeacherWriteData> teacherWriteDataList;
}
