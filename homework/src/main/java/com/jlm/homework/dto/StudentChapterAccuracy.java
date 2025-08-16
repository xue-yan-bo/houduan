package com.jlm.homework.dto;

import lombok.Data;

@Data
public class StudentChapterAccuracy {
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private String chapter;
    private Double accuracy;
}
