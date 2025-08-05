package com.jlm.homework.dto;

/**
 * 练习册验证器
 * 负责验证练习册请求参数的合法性
 */
public class ExerciseBookValidator {

    /**
     * 验证创建请求参数
     */
    public static String validateForCreate(ExerciseBookRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return "练习册标题不能为空";
        }
        if (request.getTitle().length() > 200) {
            return "练习册标题长度不能超过200个字符";
        }
        /*if (request.getDifficultyLevel() != null && (request.getDifficultyLevel() < 1 || request.getDifficultyLevel() > 5)) {
            return "难度等级必须在1-5之间";
        }*/
        return null;
    }

    /**
     * 验证更新请求参数
     */
    public static String validateForUpdate(ExerciseBookRequest request) {
        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                return "练习册标题不能为空";
            }
            if (request.getTitle().length() > 200) {
                return "练习册标题长度不能超过200个字符";
            }
        }
        if (request.getDifficultyLevel() != null && (request.getDifficultyLevel() < 1 || request.getDifficultyLevel() > 5)) {
            return "难度等级必须在1-5之间";
        }
        return null;
    }

    /**
     * 验证查询请求参数
     */
    public static String validateForQuery(ExerciseBookRequest request) {
        if (request.getPageNum() < 1) {
            return "页码必须大于0";
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 100) {
            return "每页大小必须在1-100之间";
        }
        if (request.getDifficultyLevel() != null && (request.getDifficultyLevel() < 1 || request.getDifficultyLevel() > 5)) {
            return "难度等级必须在1-5之间";
        }
        if (request.getSortDir() != null) {
            String sortDir = request.getSortDir().toLowerCase();
            if (!sortDir.equals("asc") && !sortDir.equals("desc")) {
                return "排序方向只能是asc或desc";
            }
        }
        // 验证时间区间：结束时间不能早于开始时间
        if (request.getParsedCreatedStartTime() != null && request.getParsedCreatedEndTime() != null) {
            if (request.getParsedCreatedEndTime().isBefore(request.getParsedCreatedStartTime())) {
                return "创建时间结束时间不能早于开始时间";
            }
        }
        return null;
    }
}