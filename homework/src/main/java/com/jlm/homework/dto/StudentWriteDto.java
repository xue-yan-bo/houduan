package com.jlm.homework.dto;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.StudentsWriteRecord;
import lombok.Data;

import java.util.List;

@Data
public class StudentWriteDto {
    /**
     * 学生ID
     */
    private Long studentId;

    private String studentName;
    /**
     * 学生书写笔记
     */
    private List<ClassroomStudentWriteData> studentWriteRecordlist;
}
