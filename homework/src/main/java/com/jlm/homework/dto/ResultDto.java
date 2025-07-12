package com.jlm.homework.dto;

import lombok.Data;

/**
 * 操作结果对象
 */
@Data
public class ResultDto<T> {
    //错误码
    private int code;
    //是否成功
    private boolean success;
    //错误信息
    private String errorMsg;
    //数据对象
    private T data;

    public static ResultDto<String> success(String msg){
        ResultDto<String> resultDto=new ResultDto<>();
        resultDto.setData(msg);
        return  resultDto;
    }

    public static ResultDto<String> error(int code,String errorMsg){
        ResultDto<String> resultDto=new ResultDto<>();
        resultDto.setSuccess(false);
        resultDto.setCode(code);
        resultDto.setErrorMsg(errorMsg);
        return  resultDto;
    }
}
