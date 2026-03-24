package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class CopybookStatistics {
    // 第一个接口的返回结构
    @Data
    public static class ClassCopybookStatistics {
        private Long copybookId;
        private String copybookName;
        private int totalStudents;
        private int completedStudents;
        private double completionPercentage;
    }

    // 第二个接口的返回结构
    @Data
    public static class CopybookStudentStatistics {
        private Long copybookId;
        private String copybookName;
        private int totalStudents;
        private int completedStudents;
        private double completionPercentage;
        private List<StudentCompletion> studentCompletions;
    }

    @Data
    public static class StudentCompletion {
        private Long studentId;
        private String studentName;
        private boolean completed;
    }
}