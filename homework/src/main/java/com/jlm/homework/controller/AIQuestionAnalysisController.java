package com.jlm.homework.controller;

import com.jlm.homework.dto.AIQuestionAnalysisDto;
import com.jlm.homework.entity.QuestionAnalysis;
import com.jlm.homework.service.IQuestionAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI分析作业", description = "AI分析作业")
@RestController
@RequestMapping("/api/ai-question-analysis")
public class AIQuestionAnalysisController {
    @Autowired
    private IQuestionAnalysisService questionAnalysisService;

    /**
     * 作业AI分析结果统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "作业AI分析结果统计")
    public List<AIQuestionAnalysisDto> teacherStartAnswer(@RequestParam(value = "homeworkPublishId") Long homeworkPublishId,
                                                          @RequestParam(value = "classId") Long classId) throws Throwable {
        List<AIQuestionAnalysisDto> list=questionAnalysisService.findAIQuestionStatistics(homeworkPublishId,classId);
        return list;
    }

    /**
     * 作业AI分析结果详情列表
     */
    @GetMapping("/list")
    @Operation(summary = "作业AI分析结果详情列表")
    public List<QuestionAnalysis> teacherStartAnswer(@RequestParam(value = "homeworkPublishId") Long homeworkPublishId,
                                                     @RequestParam(value = "classId") Long classId,
                                                     @RequestParam(value = "bigNumber") String bigNumber,
                                                     @RequestParam(value = "smallNumber") String smallNumber,
                                                     @RequestParam(value = "isCorrect", required = false )Boolean isCorrect) throws Throwable {
        List<QuestionAnalysis> list=questionAnalysisService.findQuestionAnalysis(homeworkPublishId,classId,bigNumber,smallNumber,isCorrect);
        return list;
    }
}
