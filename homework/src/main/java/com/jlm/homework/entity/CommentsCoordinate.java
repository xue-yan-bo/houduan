package com.jlm.homework.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class CommentsCoordinate implements Serializable {
    private Integer index;
    private Double x;
    private Double y;
    private String text;
    private Long timestamp;
}
