package com.jlm.homework.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class AuditLogoCoordinate implements Serializable {
    private Integer index;
    private Double x;
    private Double y;
    private Object symbol;
    private Boolean isStart;
    private Boolean isEnd;
    private String strokeId;
    private Integer size;
    private Long timestamp;
}
