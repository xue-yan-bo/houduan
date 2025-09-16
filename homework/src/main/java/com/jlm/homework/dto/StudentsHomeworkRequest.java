package com.jlm.homework.dto;

import lombok.Data;
@Data
public class StudentsHomeworkRequest {
    private String studentName;
    private Integer submitStatus;
    private String submitTime;
    private String auditTime;
    private String auditStatus;
}
