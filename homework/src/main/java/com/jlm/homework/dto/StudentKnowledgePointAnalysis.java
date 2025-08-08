package com.jlm.homework.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StudentKnowledgePointAnalysis implements Serializable {
    private String knowledgePoint;
    private String studentId;
    private String studentName;
    private Double masteryRate;
    private String subject;
    private Long classId;
    private String className;
}
