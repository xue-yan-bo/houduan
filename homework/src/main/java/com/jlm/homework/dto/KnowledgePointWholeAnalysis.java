package com.jlm.homework.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgePointWholeAnalysis  implements Serializable {
    /**
     * 知识点
     */
    private String knowledgePoint;
    /**
     * 掌握数量
     */
    private Integer masterQuantity;
    /**
     * 掌握率
     */
    private Double masteryRate;
}
