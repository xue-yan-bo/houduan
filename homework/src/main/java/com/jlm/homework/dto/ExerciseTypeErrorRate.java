package com.jlm.homework.dto;

import lombok.Data;

@Data
public class ExerciseTypeErrorRate {
    private String titleType;
    private Double errorRate;
    private Integer titleNum;
    private Integer rightNum;
}