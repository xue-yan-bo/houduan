package com.jlm.ai.config;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;

@Configuration
public class ZhipuAIConfig {

    @Value("${zhipu.ai.api-key:}")
    private String zhipuApiKey;
    
    @Bean
    public  ZhipuAIImageAnalysisUtil zhipuAIImageAnalysisUtil() {

        //异步处理AI智能审批
        return ZhipuAIImageAnalysisUtil.createInstance(zhipuApiKey);
    }
}