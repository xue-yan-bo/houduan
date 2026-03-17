package com.jlm.homework.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai-mid")
public class AiMidConfig {
    private String baseUrl;
    private String reviewUrl;
    private String pollUrl;
    private String historyUrl;
    private String aiCallbackUrl;
    private String model;
}
