package com.jlm.homework.dto;

import lombok.Data;

@Data
public class HomeworkRightRate {
    private String className;
    private Integer totalNum;
    private Integer rightNum;
    private Double rightRate;
    private Integer errorNum;
    private Double errorRate;
}
