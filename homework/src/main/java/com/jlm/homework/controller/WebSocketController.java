package com.jlm.homework.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 处理来自客户端的点对点消息
    @MessageMapping("/sendToUser")
    @SendTo("/queue/user/{recipient}")
    public String sendToUser(@Payload String message, @Header("recipient") String recipient) {
        return message; // 返回的消息将自动发送到指定的用户队列
    }

    // 处理来自客户端的广播消息
    @MessageMapping("/broadcast")
    @SendTo("/topic/messages")
    public String broadcastMessage(@Payload String message) {
        return message; // 返回的消息将广播到所有订阅了/topic/messages的客户端
    }

    // 通过控制器发送消息（例如，用于测试或管理目的）
    // 注意：这个方法不是通过WebSocket消息映射触发的，而是直接通过HTTP请求调用的
    // 在实际应用中，你可能会有其他服务或组件来调用这个方法
    public void sendMessageToUser(String recipient, String message) {
        messagingTemplate.convertAndSend("/queue/user/" + recipient, message);
    }

    public void broadcastMessageToAll(String message) {
        messagingTemplate.convertAndSend("/topic/messages", message);
    }
}
