package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;
@Data
public class Result <T>{
    private Integer total;
    private Integer code;
    private String msg;
    private List<T> rows;
}
