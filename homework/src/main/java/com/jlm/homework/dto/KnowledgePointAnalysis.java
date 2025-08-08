package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

/**
 * 知识点分析
 */
@Data
public class KnowledgePointAnalysis {
    /**
     * 知识点总体分析
     */
    private List<KnowledgePointWholeAnalysis> wholeAnalysisList;

    /**
     * 学生知识点分析
     */
    private List<StudentKnowledgePointAnalysis> studentKnowledgePointAnalysisList;
}
