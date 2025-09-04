package com.jlm.homework.dto;

import lombok.Data;
//各年级作业提交情况
@Data
public class GradeHomeworkSubmit {
    private String grade;
    private Double submitRate;
    private Double unSubmitRate;
}
