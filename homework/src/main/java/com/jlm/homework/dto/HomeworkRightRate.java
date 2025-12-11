package com.jlm.homework.dto;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ColumnResult;

@Data
@SqlResultSetMapping(
    name = "HomeworkRightRateMapping",
    classes = {
        @ConstructorResult(
            targetClass = HomeworkRightRate.class,
            columns = {
                @ColumnResult(name = "classId", type = Long.class),
                @ColumnResult(name = "grade", type = String.class),
                @ColumnResult(name = "totalNum", type = Integer.class),
                @ColumnResult(name = "rightNum", type = Integer.class),
                @ColumnResult(name = "errorNum", type = Integer.class)
            }
        )
    }
)
@SqlResultSetMapping(
    name = "HomeworkRightRateTotalMapping",
    classes = {
        @ConstructorResult(
            targetClass = HomeworkRightRate.class,
            columns = {
                @ColumnResult(name = "totalNum", type = Integer.class),
                @ColumnResult(name = "rightNum", type = Integer.class),
                @ColumnResult(name = "errorNum", type = Integer.class)
            }
        )
    }
)
public class HomeworkRightRate {
    private Long classId;
    private String className;
    private String grade;
    private Integer totalNum;
    private Integer rightNum;
    private Double rightRate;
    private Integer errorNum;
    private Double errorRate;
    private Double noAnswerRate;
}
