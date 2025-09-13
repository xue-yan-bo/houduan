package com.jlm.homework.dto;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

@Data
public class SubjectTimeNum {
    private String subject;
    private Integer duration;
    private Integer number;
}
