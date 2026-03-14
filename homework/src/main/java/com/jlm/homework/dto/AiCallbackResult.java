package com.jlm.homework.dto;

import lombok.Data;

import java.util.Date;
@Data
public class AiCallbackResult {
    private String businessId;//'发送给AI请求的Id'
    private String aiTaskId;// '中台任务id'
    private Integer status;///  ---- 0 =处理成功  非0 处理失败
    private String msg;  // ok 或失败原因
    private Date dealTime;
}
