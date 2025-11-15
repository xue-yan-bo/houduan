package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

/**
 * 作业学生书写数据 实体类
 */
@Data
@Entity
@Table(name = "homework_student_write_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HomeworkStudentWriteData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生作业Id
     */
    @Column(name = "student_homework_id")
    private Long studentHomeworkId;
    /**
     * 学生Id
     */
    @Column(name = "student_id")
    private Long studentId;

    @Transient
    private String studentName;

    /**
     * 页数
     */
    @Column(name = "page_num")
    private Integer pageNum;
    /**
     * 下标
     */
    @Column(name = "index_n")
    private Integer indexN;

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

    /**
     * 类型,1作业答题 , 2作业订正
     */
    @Column(name = "type_n")
    private String type;

    /**
     * 偏移量
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offset", columnDefinition = "JSON")
    private OffsetEntity offset;
}
