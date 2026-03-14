package com.jlm.homework.controller;

import com.jlm.homework.dto.AiCallbackResult;
import com.jlm.homework.dto.AiCallbackRequest;
import com.jlm.homework.service.IAiCallbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI中台回调", description = "AI中台回调相关的接口")
@RestController
@RequestMapping("/api/aicallback")
public class AICallbackController {
    @Autowired
    private IAiCallbackService aiCallbackService;
    @PostMapping
    public AiCallbackResult aicallback(@RequestBody AiCallbackRequest aiCallbackRequest){
        AiCallbackResult result = new AiCallbackResult();

        result =aiCallbackService.aicallback(aiCallbackRequest);
        return result;
    }
}
