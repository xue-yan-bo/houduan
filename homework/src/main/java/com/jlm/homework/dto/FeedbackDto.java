package com.jlm.homework.dto;

import lombok.Data;

@Data
public class FeedbackDto {
    private String feedbackDate;
    private Long classId;
    private String className;
    private Integer num;
}
