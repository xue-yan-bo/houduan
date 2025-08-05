package com.jlm.homework.dto;

import java.util.Map;

public class StudentAnswerDto {
    /**
     * 题号
     */
    private Integer titleNumber;
    /**
     * 答题人数
     */
    private Integer answerStudentNumber;
    /**
     * 未答题人数
     */
    private Integer unanswerStudentNumber;
    /**
     * 答对人数
     */
    private Integer rightNumber;
    /**
     * 答错人数
     */
    private Integer wrongNumber;
    /**
     * 正确答案
     */
    private String correctAnswer;
    /**
     * 各种答案占比
     */
    private Map<String,Double> variousAnswersProp;
}
