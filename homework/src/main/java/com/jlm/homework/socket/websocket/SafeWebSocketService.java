package com.jlm.homework.socket.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SafeWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public SafeWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 安全地发送消息到指定用户
     */
    public void sendToUser(String userId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
        } catch (Exception e) {
            // 捕获会话关闭等异常，避免影响其他操作
            log.debug("Failed to send message to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 安全地发送消息到指定主题
     */
    public void sendToTopic(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            log.debug("Failed to send message to topic {}: {}", topic, e.getMessage());
        }
    }
}
