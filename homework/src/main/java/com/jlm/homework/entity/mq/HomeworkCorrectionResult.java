package com.jlm.homework.entity.mq;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime correctedAt;
    private CorrectionStatus status;
    private String  type;
    private String  errorMessage;
}
