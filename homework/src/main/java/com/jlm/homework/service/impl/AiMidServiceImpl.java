package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
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

    private static final String REVIEW_ROLE = "b3f1c2d4e5f60718293a4b5c6d7e8f90";
    private static final String DEFAULT_ROLE = "0f1e2d3c4b5a69788796a5b4c3d2e1f0";

    private final AiMidConfig aiMidConfig;

    public AiMidServiceImpl(AiMidConfig aiMidConfig) {
        this.aiMidConfig = aiMidConfig;
    }

    @Override
    public AIMidDto apiReview(String businessId, List<AiFile> fileList, String businessType, String prompt) {
        if (StringUtils.isEmpty(businessId)) {
            throw new IllegalArgumentException("businessId is empty");
        }
        if (fileList == null || fileList.isEmpty()) {
            throw new IllegalArgumentException("fileList is empty");
        }
        if (aiMidConfig == null) {
            throw new IllegalStateException("AiMidConfig is null");
        }

        AiMidRequest request = new AiMidRequest();
        request.setRequestId(businessId);
        request.setMode("async");
        if ("作业批改".equals(businessType)) {
            request.setRole(REVIEW_ROLE);
        } else {
            request.setRole(DEFAULT_ROLE);
            request.setPrompt(prompt);
        }
        request.setProvider(aiMidConfig.getModel());
        request.setFile_list(fileList);
        AiCallbackConfig callback = new AiCallbackConfig();
        callback.setUrl(aiMidConfig.getAiCallbackUrl());
        callback.setBusinessId(businessId);
        callback.setBusinessType(businessType);
        request.setCallback(callback);

        String url = aiMidConfig.getBaseUrl() + aiMidConfig.getReviewUrl();
        try {
            log.info("中台review入参: {}", JSON.toJSONString(request));
            String rs = HttpUtil.sendPostRequest(url, JSON.toJSONString(request));
            log.info("调用中台返回: {}", rs);
            return JSON.parseObject(rs, AIMidDto.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("调用中台review失败", e);
        }
    }

    @Override
    public AiPollStatusDto getStatusByTask(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new IllegalArgumentException("taskId is empty");
        }
        if (aiMidConfig == null) {
            throw new IllegalStateException("AiMidConfig is null");
        }
        String url = aiMidConfig.getBaseUrl() + aiMidConfig.getPollUrl() + "taskId=" + taskId;
        try {
            String rs = HttpUtil.sendGetRequest(url);
            log.info("AI分析状态: {}", rs);
            return JSON.parseObject(rs, AiPollStatusDto.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("查询AI任务状态失败", e);
        }

    }
}
