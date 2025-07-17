package com.jlm.homework.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.util.Date;
@Data
public class StudentsHomeworkRequest {
    private String studentName;
    private Integer submitStatus;
    private String submitTime;
    private String auditTime;
}
