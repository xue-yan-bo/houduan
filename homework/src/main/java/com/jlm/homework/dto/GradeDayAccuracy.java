package com.jlm.homework.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeDayAccuracy {
    private String grade;
    private String day;
    private Double accuracy;
}
