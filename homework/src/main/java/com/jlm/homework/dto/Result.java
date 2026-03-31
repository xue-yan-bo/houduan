package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;
@Data
public class Result <T>{
    private Integer total;
    private Integer code;
    private String msg;
    private List<T> rows;

    public static <T> Result<T> success(String msg) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> success(String msg, List<T> rows) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setRows(rows);
        return result;
    }
}
