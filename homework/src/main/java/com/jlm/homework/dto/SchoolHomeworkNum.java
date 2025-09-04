package com.jlm.homework.dto;

import lombok.Data;

@Data
public class SchoolHomeworkNum {
    private Long schoolId;
    private String schoolName;
    private Integer studentNum;
    private Integer homeworkNum;
    private Double homeworkAverageDuration;
}
