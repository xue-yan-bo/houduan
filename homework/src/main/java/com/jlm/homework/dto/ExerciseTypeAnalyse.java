package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExerciseTypeAnalyse {
    private Map<String,Integer> exerciseTypeMap;

    private List<DayExerciseTypeNum> dayExerciseTypeNumList;

    private List<ExerciseTypeErrorRate>  typeErrorRateList;
}
