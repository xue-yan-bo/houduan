package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class CopybookStatistics {
    @Data
    public static class ClassCopybookStatistics {
        private Long copybookId;
        private String copybookName;
        private int totalStudents;
        private int completedStudents;
        private double completionPercentage;
    }

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