package com.jlm.homework.dto;

import lombok.Data;
import java.util.List;

@Data
public class SimilarityRequestDto {

    private String targetText;

    private List<HistoryTextDto> historyTexts;

    @Data
    public static class HistoryTextDto {
        private Long id;
        private String text;
    }
}
