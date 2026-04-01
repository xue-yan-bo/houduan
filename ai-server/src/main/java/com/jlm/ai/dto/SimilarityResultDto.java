package com.jlm.ai.dto;

import lombok.Data;

@Data
public class SimilarityResultDto {
    /**
     * 算出来的最高相似度（如 0.85）
     */
    private Double maxSimilarity;

    /**
     * 命中的、相似度最高的那道历史题的ID（如果没找到或者没命中则为空）
     */
    private Long duplicateOf;
}