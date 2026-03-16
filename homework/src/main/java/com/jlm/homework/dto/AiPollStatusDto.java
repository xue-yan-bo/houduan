package com.jlm.homework.dto;

import lombok.Data;

@Data
public class AiPollStatusDto {
    private String msg;//": "排队中（位置未知）",
    //pending、finished
    private String status;//": "pending",
    private String taskId;
}
