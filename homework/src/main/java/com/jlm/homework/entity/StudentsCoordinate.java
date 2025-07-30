package com.jlm.homework.entity;

import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
@Data
public class StudentsCoordinate implements Serializable {
    private Integer index;
    private Double x;
    private Double y;
    private String text;
}
