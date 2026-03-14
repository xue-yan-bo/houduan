package com.jlm.homework.service.impl;

import com.jlm.homework.dto.AiCallbackRequest;
import com.jlm.homework.dto.AiCallbackResult;
import com.jlm.homework.entity.AiCallback;
import com.jlm.homework.repository.AiCallbackRepository;
import com.jlm.homework.service.IAiCallbackService;
import com.tencentcloudapi.lke.v20231130.models.AICallConfig;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AiCallbackServiceImpl implements IAiCallbackService {
    @Resource
    private AiCallbackRepository aiCallbackRepository;

    @Override
    public AiCallbackResult aicallback(AiCallbackRequest aiCallbackRequest) {
        AiCallbackResult result = new AiCallbackResult();
        result.setAiTaskId(aiCallbackRequest.getAiTaskId());
        result.setBusinessId(aiCallbackRequest.getBusinessId());
        AiCallback aiCallback = new AiCallback();
        BeanUtils.copyProperties(aiCallbackRequest, aiCallback);
        aiCallback.setCreateTime(new Date());
        aiCallbackRepository.save(aiCallback);

        //业务处理
        if("作业批改".equals(aiCallbackRequest.getBusinessType())){

        }else{//随堂检测

        }
        result.setStatus(0);
        result.setMsg("OK");
        result.setDealTime(new Date());
        return result;
    }
}
