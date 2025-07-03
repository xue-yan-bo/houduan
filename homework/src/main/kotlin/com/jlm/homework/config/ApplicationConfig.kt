package com.jlm.homework.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.ZoneId
import java.util.*

/**
 * 应用程序配置类
 * 提供全局配置和Bean定义
 */
@Configuration
class ApplicationConfig {

    /**
     * 配置Jackson ObjectMapper
     * 支持Kotlin数据类和时区设置
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        return builder
            .timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
            .build<ObjectMapper>()
            .registerKotlinModule()
    }

    /**
     * 时区配置Bean
     */
    @Bean
    fun timeZone(): TimeZone {
        return TimeZone.getTimeZone(ZoneId.systemDefault())
    }
}
