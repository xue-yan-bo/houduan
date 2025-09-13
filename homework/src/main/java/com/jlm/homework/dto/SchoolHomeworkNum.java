package com.jlm.homework.dto;

import lombok.Data;

@Data
public class SchoolHomeworkNum {
    private Long schoolId;
    private String schoolName;
    private String schoolAdress;
    private Integer studentNum;
    private Integer homeworkNum;
    private Double homeworkAverageDuration;
}
