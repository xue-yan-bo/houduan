package com.jlm.homework.config;

import com.jlm.homework.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置拦截器等Web相关组件
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private TokenInterceptor tokenInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Token拦截器
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**") // 拦截所有API请求
                .excludePathPatterns("/api/public/**") // 排除公开接口
                .excludePathPatterns("/swagger-ui/**") // 排除Swagger相关接口
                .excludePathPatterns("/v3/api-docs/**") // 排除API文档接口
                .excludePathPatterns("/actuator/**"); // 排除监控接口
    }
}