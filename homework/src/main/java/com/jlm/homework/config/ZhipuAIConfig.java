package com.jlm.homework.config;

import com.jlm.homework.util.ZhipuAIImageAnalysisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZhipuAIConfig {

    @Value("${zhipu.ai.api-key:}")
    private String zhipuApiKey;
    
    @Bean
    public  ZhipuAIImageAnalysisUtil zhipuAIImageAnalysisUtil() {
        return ZhipuAIImageAnalysisUtil.createInstance(zhipuApiKey);
    }
}