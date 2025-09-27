package com.jlm.homework.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class AuditCoordinate implements Serializable {
    private Integer index;
    private Double x;
    private Double y;
    private Boolean isStart;
    private Boolean isEnd;
    private String strokeId;
}
