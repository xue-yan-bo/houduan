package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class AccuracyDto {
    private List<AverageAccuracyDto> averageAccuracyDtos;

    private List<SubjectAccuracyDto> subjectAccuracyDtos;
}
