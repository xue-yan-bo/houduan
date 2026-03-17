package com.jlm.homework.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class HomeworkAISmallDto implements Serializable {
    private String smallNumber;
    private String correctFlag;
    private String parse;
}
