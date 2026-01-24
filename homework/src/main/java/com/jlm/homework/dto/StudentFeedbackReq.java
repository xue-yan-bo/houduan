package com.jlm.homework.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jlm.homework.entity.StudentsWriteRecord;
import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

@Data
public class StudentFeedbackReq {

    private Long studentId;
    /**
     * 学生名称
     */
    private String studentName;
    /**
     * 学校Id
     */
    private Long schoolId;
    /**
     * 班级Id
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;
    /**
     * 学科
     */
    private String subject;

    /**
     * 反馈时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String feedbackTimeStart;

    /**
     * 反馈时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String feedbackTimeEnd;
}
