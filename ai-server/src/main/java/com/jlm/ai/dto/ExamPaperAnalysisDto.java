package com.jlm.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.util.List;
/**
 * 试卷分析结果
 * 用于封装整份试卷的分析信息
 */
@Data
public class ExamPaperAnalysisDto {
    private String paperName;          // 试卷名称
    private int totalPages;            // 总页数
    private int totalQuestions;        // 总题数
    private int totalScore;            // 总分值
    private List<QuestionAnalysisDto> questions; // 试题列表
    private String overallEvaluation;  // 总体评价
    private String gradingSummary;     // 评分总结
    private String suggestions;        // 改进建议
    private String analysisTime;       // 分析时间

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("试卷分析结果：\n")
                .append("试卷名称: ").append(paperName).append("\n")
                .append("总页数: ").append(totalPages).append("\n")
                .append("总题数: ").append(totalQuestions).append("\n")
                .append("总分值: ").append(totalScore).append("\n")
                .append("试题详情: \n");

        for (QuestionAnalysisDto q : questions) {
            sb.append("  - ").append(q.toString()).append("\n");
        }

        sb.append("总体评价: ").append(overallEvaluation).append("\n")
                .append("评分总结: ").append(gradingSummary).append("\n")
                .append("改进建议: ").append(suggestions).append("\n")
                .append("分析时间: ").append(analysisTime);

        return sb.toString();
    }

    /**
     * 将分析结果转换为JSON字符串
     * @param objectMapper Jackson的ObjectMapper实例
     * @return JSON格式的字符串
     * @throws IOException JSON序列化异常
     */
    public String toJson(ObjectMapper objectMapper) throws IOException {
        return objectMapper.writeValueAsString(this);
    }
}
