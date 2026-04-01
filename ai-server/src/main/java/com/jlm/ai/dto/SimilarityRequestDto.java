package com.jlm.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class SimilarityRequestDto {
    /**
     * 当前需要判断查重的新题目文本
     */
    private String targetText;

    /**
     * 该班级或该个人的所有历史题目文本与ID列表
     */
    private List<HistoryTextDto> historyTexts;

    @Data
    public static class HistoryTextDto {
        private Long id;
        private String text;
        private Integer errorCount;
    }
}