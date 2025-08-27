package com.jlm.socketserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 应用程序配置类
 * 提供全局配置和Bean定义
 */
@Configuration
public class ApplicationConfig {
    /**
     * 配置Jackson ObjectMapper
     * 支持时区设置
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
                .build();
    }

    /**
     * 时区配置Bean
     */
    @Bean
    public TimeZone timeZone() {
        return TimeZone.getTimeZone(ZoneId.systemDefault());
    }
}
