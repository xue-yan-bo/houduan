package com.jlm.homework.service.impl;

import com.alibaba.fastjson2.JSON;
import com.jlm.homework.config.AiMidConfig;
import com.jlm.homework.dto.*;
import com.jlm.homework.service.IAiMidService;
import com.jlm.homework.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
@Slf4j
@Service
public class AiMidServiceImpl implements IAiMidService {

    private final AiMidConfig aiMidConfig;

    public AiMidServiceImpl(AiMidConfig aiMidConfig) {
        this.aiMidConfig = aiMidConfig;
    }

    @Override
    public AIMidDto apiReview(String businessId, List<AiFile> fileList, String businessType, String prompt) {
        AiMidRequest request = new AiMidRequest();
        request.setRequestId(businessId);
        request.setMode("async");
        if ("作业批改".equals(businessType)) {
            request.setRole("b3f1c2d4e5f60718293a4b5c6d7e8f90");
        } else {
            request.setRole("0f1e2d3c4b5a69788796a5b4c3d2e1f0");
            request.setPrompt(prompt);
        }
        request.setProvider(aiMidConfig.getModel());
        request.setFile_list(fileList);
        AiCallbackConfig callback = new AiCallbackConfig();
        callback.setUrl(aiMidConfig.getAiCallbackUrl());
        callback.setBusinessId(businessId);
        callback.setBusinessType(businessType);
        request.setCallback(callback);
        try {
            String rs = HttpUtil.sendPostRequest(aiMidConfig.getBaseUrl() + aiMidConfig.getReviewUrl(), JSON.toJSONString(request));
            log.info("调用中台开始：" + rs);
            AIMidDto dto = JSON.parseObject(rs, AIMidDto.class);
            return dto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AiPollStatusDto getStatusByTask(String taskId) {
        String url = aiMidConfig.getBaseUrl() + aiMidConfig.getPollUrl() + "taskId=" + taskId;
        String rs = null;
        try {
            rs = HttpUtil.sendGetRequest(url);
            log.info("AI分析状态：" + rs);
            AiPollStatusDto dto = JSON.parseObject(rs, AiPollStatusDto.class);
            return dto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
