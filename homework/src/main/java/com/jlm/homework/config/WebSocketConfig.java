package com.jlm.homework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * ServerEndpointExporter 负责扫描和注册所有带有 @ServerEndpoint 注解的 WebSocket 端点。
     * 如果使用独立的servlet容器，则无需提供此Bean。
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/ws");
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/websocket")
                .setAllowedOrigins(
                "http://localhost",
                "http://localhost:81",
                "http://localhost:82",
                "http://localhost:83",
                "http://192.168.1.129",
                "http://192.168.1.129:81",
                "http://192.168.1.129:82",
                "http://192.168.1.129:83",
                "ws://192.168.1.129",
                "http://127.0.0.1",
                "http://192.168.1.135",  // 添加您的IP地址
                "ws://192.168.1.135",
                "http://192.168.1.135:18080",
                "http://49.232.159.98",
                "http://192.168.1.251",
                "http://192.168.1.29",
                "http://192.168.1.29:99",
                "http://49.232.159.98:9000"  // 添加 Vue 应用的地址
        ).withSockJS();
    }

}
