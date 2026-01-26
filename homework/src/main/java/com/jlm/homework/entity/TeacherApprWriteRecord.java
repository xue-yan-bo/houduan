package com.jlm.homework.entity;

import lombok.Data;

@Data
public class TeacherApprWriteRecord {
    private Integer x;              // X坐标
    private Integer y;              // Y坐标
    private Integer pressure;       // 压力值
    private Long timestamp;     // 时间戳（ms，手写板开机起始）
    private Boolean isStartOfStroke;
    private Boolean isEndOfStroke;
    private Integer strokeInde;
    private String color;
    private Integer width;
    private Boolean isEraser;

}
