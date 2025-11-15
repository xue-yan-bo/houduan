package com.jlm.homework.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "wrong_title_write_data")
public class WrongTitleWriteData implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;
    /**
     * 科目
     */
    @Column(name = "subject")
    private String subject;

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
    private Date createTime;
}
