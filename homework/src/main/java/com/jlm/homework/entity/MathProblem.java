package com.jlm.homework.entity;

import lombok.Data;

/**
 * 数学题目实体类
 */
@Data
public class MathProblem {
    private Integer bigQuestionNum; // 大题号
    private Integer smallQuestionNum; // 小题号
    private String content; // 题内容
    private String type; // 题类型（填空、判断、选择等）
    private String knowledgePoint; // 考察知识点
    private String correctness; // 答题是否正确
    private String answer; // 参考答案
    private String analysis; // 解析
    private String options; // 选择题选项（可选）
}