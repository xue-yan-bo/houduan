package com.jlm.homework.dto;

import com.jlm.homework.entity.WrongTitleStatistics;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import lombok.Data;

import java.util.List;

/**
 * 学生错题分析
 */
@Data
public class StudentsWrongTitleAnalyse {
    /**
     * 错题量
     */
    @Column(name = "wrong_title_num")
    private Integer wrongTitleNum;
    /**
     * 总题量
     */
    @Column(name = "title_total")
    private Integer titleTotal;
    /**
     * 平均正确率
     */
    @Column(name = "average_correctness")
    private Double averageCorrectness;
    /**
     * 最高正确率
     */
    @Column(name = "max_correctness")
    private Double maxCorrectness;
    /**
     * 最低正确率
     */
    @Column(name = "min_correctness")
    private Double minCorrectness;
    /**
     * 错题本
     */
    @Transient
    private List<WrongTitleStatistics> wrongTitleBooks;
}
