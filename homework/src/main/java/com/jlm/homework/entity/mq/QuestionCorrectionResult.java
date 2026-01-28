package com.jlm.homework.entity.mq;

import lombok.Data;

@Data
public class QuestionCorrectionResult {
    private String questionNumber;
    private String questionText;
    private String questionType;
    private String studentAnswer;
    private String standardAnswer;
    private Boolean isCorrect;
    private Double score;
    private Double  maxScore;
    private String feedback;
}
