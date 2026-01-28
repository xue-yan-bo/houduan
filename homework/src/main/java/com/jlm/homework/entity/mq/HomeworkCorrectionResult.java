package com.jlm.homework.entity.mq;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeworkCorrectionResult {
    private String submissionId;
    private String homeworkId;
    private String studentId;
    private List<QuestionCorrectionResult> questions;
    private Double totalScore;
    private Double maxScore;
    private LocalDateTime correctedAt;
    private CorrectionStatus status;
    private String  type;
    private String  errorMessage;
}
