package com.jlm.homework.dto;

import lombok.Data;

import java.util.Date;
@Data
public class TodayHomeworkSubmit {
    private String className;
    private Integer studentNum;
    private Integer submitNum;
    private Double submitRate;
    private Date deadline;
}
