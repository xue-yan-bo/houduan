package com.jlm.homework.dto;

import lombok.Data;

@Data
public class ClassroomExercisesData {
    /**
     * 题号
     */
    private Integer titleNumber;
    /**
     * 正确答案
     */
    private String answer;
    /**
     * 作答人数
     */
    private Integer answerNum;
    /**
     * 未作答人数
     */
    private Integer unAnswerNum;
    /**
     * 答对人数
     */
    private Integer rightNum;
    /**
     * 答错人数
     */
    private Integer errorNum;
}
