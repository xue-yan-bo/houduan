package com.jlm.homework.socket.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MessageQueueService {
    @Autowired
    private SafeWebSocketService safeWebSocketService;
    private final Map<String, Queue<Object>> userMessageQueues = new ConcurrentHashMap<>();

    /**
     * 添加消息到用户队列
     */
    public void addMessage(String userId, Object message) {
        userMessageQueues.computeIfAbsent(userId, k -> new LinkedList<>()).add(message);
    }

    /**
     * 处理用户连接时的消息发送
     */
    public void processQueuedMessages(String userId) {
        Queue<Object> queue = userMessageQueues.remove(userId);
        if (queue != null && !queue.isEmpty()) {
            log.info("Processing {} queued messages for user {}", queue.size(), userId);
            // 发送队列中的消息
            for (Object message : queue) {
                safeWebSocketService.sendToUser(userId, "/queue/messages", message);
            }
        }
    }

    /**
     * 清理用户队列
     */
    public void clearQueue(String userId) {
        userMessageQueues.remove(userId);
    }
}
