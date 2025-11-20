package com.jlm.homework.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.util.Objects;

@Data
public class AIQuestionAnalysisDto {

    private String bigNumber;          // 大题号

    private String smallNumber;

    private Integer errorNum;

    private Integer correctNum;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AIQuestionAnalysisDto that)) return false;
        return Objects.equals(bigNumber, that.bigNumber) && Objects.equals(smallNumber, that.smallNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigNumber, smallNumber);
    }
}
