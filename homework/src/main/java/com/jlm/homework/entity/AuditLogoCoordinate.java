package com.jlm.homework.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class AuditLogoCoordinate implements Serializable {
    private Integer index;
    private Double x;
    private Double y;
    private Object symbol;
}
