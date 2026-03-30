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
@Table(name = "classroom_tearcher_approve_stu")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassroomTearcherApproveStu {
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

    @Column(name = "student_name")
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
     * 老师审批学生记录
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tearcher_appr_stu_data", columnDefinition = "JSON")
    private List<TeacherApprWriteRecord> tearcherApprStuData;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 课堂联系Id
     */
    @Column(name = "classroom_exercises_id")
    private Long classroomExercisesId;

    /**
     * 课堂联系Id
     */
    @Column(name = "teacher_id")
    private Long teacherId;
}
