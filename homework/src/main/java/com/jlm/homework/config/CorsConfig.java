package com.jlm.homework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("http://localhost:81");
        config.addAllowedOrigin("http://localhost:82");
        config.addAllowedOrigin("http://localhost:83");
        config.addAllowedOrigin("http://192.168.1.129");
        config.addAllowedOrigin("http://192.168.1.129:81");
        config.addAllowedOrigin("http://192.168.1.129:82");
        config.addAllowedOrigin("http://192.168.1.129:83");
        config.addAllowedOrigin("ws://192.168.1.129");
        config.addAllowedOrigin("http://127.0.0.1");
        config.addAllowedOrigin("http://192.168.1.135");
        config.addAllowedOrigin("ws://192.168.1.135");
        config.addAllowedOrigin("http://192.168.1.135:18080");
        config.addAllowedOrigin("http://49.232.159.98:9000");
        config.addAllowedOrigin("http://49.232.159.98");
        
        // 允许的方法
        config.addAllowedMethod("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许凭证
        config.setAllowCredentials(true);
        
        // 预检请求的有效期（秒）
        config.setMaxAge(3600L);
        
        // 应用到所有路径
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}