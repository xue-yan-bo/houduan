package com.jlm.homework.dto;

import lombok.Data;

@Data
public class SimilarityResultDto {
    private Double maxSimilarity;
    private Long duplicateOf;
}
