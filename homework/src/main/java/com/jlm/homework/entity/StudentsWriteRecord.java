package com.jlm.homework.entity;

import java.io.Serializable;
import lombok.Data;

@Data
public class StudentsWriteRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer x;              // X坐标
    private Integer y;              // Y坐标
    private Integer pressure;       // 压力值
    private Long timestamp;     // 时间戳（ms，手写板开机起始）

    // 构造方法
    public StudentsWriteRecord() {
    }

    public StudentsWriteRecord(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public StudentsWriteRecord(Integer x, Integer y, Integer pressure, Long timestamp) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.timestamp = timestamp;
    }
}
