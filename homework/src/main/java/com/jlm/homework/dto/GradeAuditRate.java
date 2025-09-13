package com.jlm.homework.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeAuditRate {
    private String grade;
    private String day;
    private BigDecimal auditRate;
}
