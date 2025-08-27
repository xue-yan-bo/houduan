package com.jlm.homework.dto;

import lombok.Data;

import java.util.Map;

/**
 * 课堂数据
 */
@Data
public class TeacherClassroomData {
    /**
     * 上课次数
     */
    private Integer classesNum;
    /**
     * 课堂练习次数
     */
    private Integer classroomExercisesNum;
    /**
     * 纯笔耕次数
     */
    private Integer purePenPlowNum;

    /**
     * 课堂互动使用次数
     */
    private Integer classroomInteractionNum;

    /**
     * 课堂互动使用次数
     */
    private Map<String,Integer> dayClassroomUseNumMap;
}
