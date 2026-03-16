package com.jlm.homework.controller;

import com.jlm.homework.dto.AiCallbackResult;
import com.jlm.homework.dto.AiCallbackRequest;
import com.jlm.homework.dto.AiPollStatusDto;
import com.jlm.homework.service.IAiCallbackService;
import com.jlm.homework.service.IAiMidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI中台回调", description = "AI中台回调相关的接口")
@RestController
@RequestMapping("/api/aicallback")
public class AICallbackController {
    @Autowired
    private IAiCallbackService aiCallbackService;
    @Autowired
    private IAiMidService aiMidService;
    @PostMapping
    public AiCallbackResult aicallback(@RequestBody AiCallbackRequest aiCallbackRequest){
        AiCallbackResult result = new AiCallbackResult();

        result =aiCallbackService.aicallback(aiCallbackRequest);
        return result;
    }

    @GetMapping("/getStatus")
    @Operation(summary = "查询AI调用状态",description = "从中台查询AI轮询调用排队状态的结果")
    public AiPollStatusDto getStatusByTask(String taskId){
        AiPollStatusDto statusDto = aiMidService.getStatusByTask(taskId);
        return statusDto;
    }
}
