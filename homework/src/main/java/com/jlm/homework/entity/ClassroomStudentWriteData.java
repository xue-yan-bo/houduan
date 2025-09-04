package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

/**
 * 课堂学生书写数据 实体类
 */
@Data
@Entity
@Table(name = "classroom_student_write_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomStudentWriteData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生答题记录Id
     */
    @Column(name = "student_record_id")
    private Long studentRecordId;
    /**
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;

    /**
     * 页数
     */
    @Column(name = "page_num")
    private Integer pageNum=0;
    /**
     * 下标
     */
    @Column(name = "index_n")
    private Integer indexN=0;

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

