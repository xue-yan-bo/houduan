package com.jlm.homework.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Data
public class HomeworkAIBigDto implements Serializable {
    private String bigNumber;
    private String questionType;       // 题型
    private List<HomeworkAISmallDto> smallDtoList;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HomeworkAIBigDto that)) return false;
        return Objects.equals(bigNumber, that.bigNumber) && Objects.equals(questionType, that.questionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigNumber, questionType);
    }
}
