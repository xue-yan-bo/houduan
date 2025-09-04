package com.jlm.homework.dto;

import lombok.Data;

@Data
public class ExerciseBookUseDate {
    private Integer exerciseBookNum;
    private Integer exerciseBookUsedum;
    private String maxUsedBookName;
    private String minUsedBookName;
}
