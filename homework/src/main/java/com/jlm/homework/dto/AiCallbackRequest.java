package com.jlm.homework.dto;

import lombok.Data;

@Data
public class AiCallbackRequest {
    private String businessId;   //'业务Id'
    private String businessType; //'业务类型，作业批改、随堂检测'
    private String aiTaskId;     //'中台任务id'
    private String aiResult;      //'大模型返回结果'
}
