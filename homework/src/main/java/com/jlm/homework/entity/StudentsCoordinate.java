package com.jlm.homework.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class StudentsCoordinate implements Serializable {
    private Integer x;              // X坐标
    private Integer y;              // Y坐标
    private Integer pressure;       // 压力值
    private Long timestamp;     // 时间戳（ms，手写板开机起始）
}
