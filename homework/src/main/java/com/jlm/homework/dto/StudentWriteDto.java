package com.jlm.homework.dto;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import jakarta.persistence.Column;
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
     * 答题水平，如优秀、良好
     */
    private String answerLevel;
    /**
     * 老师评语
     */
    private String teacherComment;
    /**
     * 学生书写笔记
     */
    private List<ClassroomStudentWriteData> studentWriteRecordList;
}
