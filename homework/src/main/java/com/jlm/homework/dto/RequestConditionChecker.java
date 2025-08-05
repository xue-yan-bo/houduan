package com.jlm.homework.dto;

/**
 * 请求条件检查器
 * 负责检查请求中是否包含有效的查询条件
 */
public class RequestConditionChecker {

    /**
     * 检查是否有非空的查询条件
     */
    public static boolean hasSearchConditions(ExerciseBookRequest request) {
        return (request.getTitle() != null && !request.getTitle().isBlank()) ||
               (request.getSubject() != null && !request.getSubject().isBlank()) ||
               request.getSubjectId() != null ||
               (request.getGrade() != null && !request.getGrade().isBlank()) ||
               request.getGradeId() != null ||
               request.getClassId() != null ||
               request.getDifficultyLevel() != null ||
               request.getCreatorId() != null ||
               request.getSchoolId() != null ||
               request.getParsedCreatedStartTime() != null ||
               request.getParsedCreatedEndTime() != null;
    }
}