package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "student_feedback_write_data")
public class StudentFeedBackWriteData implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 反馈ID
     */
    @Column(name = "student_feedback_id")
    private Long studentFeedbackId;

    /**
     * 学生写作记录
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "students_write_records", columnDefinition = "JSON")
    private List<StudentsWriteRecord> studentsWriteRecords;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
