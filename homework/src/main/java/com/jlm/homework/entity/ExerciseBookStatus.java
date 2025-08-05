package com.jlm.homework.entity;



/**
 * 练习册状态枚举
 */
public enum ExerciseBookStatus {
    /**
     * 活跃状态
     */
    ACTIVE("ACTIVE","活跃状态"),

    /**
     * 非活跃状态
     */
    INACTIVE("INACTIVE","非活跃状态"),

    /**
     * 已删除状态
     */
    DELETED("DELETED","已删除状态");
    private String code;
    private String name;
    ExerciseBookStatus(String code,String name){
        this.name = name;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

}
