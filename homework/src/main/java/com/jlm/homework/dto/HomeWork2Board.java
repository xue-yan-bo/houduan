package com.jlm.homework.dto;

import com.jlm.homework.socket.SubjectParseResult;
import lombok.Data;

import java.io.Serializable;

@Data
public class HomeWork2Board implements Serializable {
    private Long homeworkId;
    private String homeworkName;
    private String subject;
    private Integer pageSize;
    private Integer titleNum;
}
