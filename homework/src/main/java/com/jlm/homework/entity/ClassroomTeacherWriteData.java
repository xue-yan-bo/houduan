package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "classroom_teacher_write_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomTeacherWriteData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 学生答题记录Id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;
    /**
     * 学生Id
     */
    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "teacher_name")
    private String teacherName;

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
    @Column(name = "teacher_write_records", columnDefinition = "JSON")
    private List<StudentsWriteRecord> teacherWriteRecords;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}

