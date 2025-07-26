package com.jlm.homework.dto;

public enum DifficultyEnum {
    Easy(1,"易"),
    RelativelyEasy(2,"较易"),
    Medium(3,"中等"),
    RelativelyDifficult(4,"较难"),
    Difficult(5,"难");
    private Integer code;
    private String name;



    DifficultyEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

}
