package com.jlm.homework.service.impl;

import com.alibaba.fastjson2.JSON;
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
    //private static String AI_BASE_URL = "http://192.168.1.156:18081";
    private static String AI_BASE_URL = "http://192.168.1.243:18081";
    private static String REVIEW_URL = "/api/review";
    private static String POLL_URL = "/api/poll?";
    private static String HISTORY_URL = "/api/history?";
    private static String AI_CALLBACK_URL = "http://192.168.1.29/prod-api/homework-board/api/aicallback";
    //private static String AI_CALLBACK_URL = "http://192.168.1.135:18080/api/aicallback";
    @Override
    public AIMidDto apiReview(String businessId, List<AiFile> fileList, String businessType,String prompt) {
        AiMidRequest request = new AiMidRequest();
        request.setRequestId(businessId);
        request.setMode("async");
        if("作业批改".equals(businessType)){
            request.setRole("homework");
        }else{
            request.setRole("quiz");
            request.setPrompt(prompt);
        }
        request.setProvider("qwen3-vl-plus-2025-12-19");
        //request.setProvider("glm-4.6v-flash");
        request.setFile_list(fileList);
        AiCallbackConfig callback = new AiCallbackConfig();
        callback.setUrl(AI_CALLBACK_URL);
        callback.setBusinessId(businessId);
        callback.setBusinessType(businessType);
        request.setCallback(callback);
        try {
            String rs =HttpUtil.sendPostRequest(AI_BASE_URL+REVIEW_URL, JSON.toJSONString(request));
            log.info("调用中台开始："+rs);
            AIMidDto dto = JSON.parseObject(rs,AIMidDto.class);
            return dto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AiPollStatusDto getStatusByTask(String taskId) {
        String url = AI_BASE_URL+POLL_URL+"?taskId="+taskId;
        String rs = null;
        try {
            rs = HttpUtil.sendGetRequest(url);
            log.info("AI分析状态："+rs);
            AiPollStatusDto dto = JSON.parseObject(rs,AiPollStatusDto.class);
            return dto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
