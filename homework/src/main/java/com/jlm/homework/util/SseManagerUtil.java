package com.jlm.homework.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jlm
 */
@Component
@Slf4j
public class SseManagerUtil {
    private static final long TIMEOUT = 0L;
    private static final int MAX_RETRY = 5;
    // 10 seconds
    private final Map<String, SseEmitter> sseCache = new ConcurrentHashMap<>();
    private static final long RETRY_INTERVAL = 10000;

    public SseEmitter createSseConnect(String clientId) {
        SseEmitter sseEmitter = new SseEmitter(TIMEOUT);
        sseEmitter.onCompletion(completionCallBack(clientId));
        sseCache.put(clientId, sseEmitter);
        log.info("为用户创建新的SSE连接：" + clientId);

        try {
            sseEmitter.send(SseEmitter.event().id(clientId).data(clientId));
        } catch (Exception e) {
            log.info(String.format("创建长连接时出错，客户端ID：%s", clientId));
            throw new RuntimeException(e);
        }
        return sseEmitter;
    }

    public String sendMsgToClient(String clientId, String msg) {
        if (sseCache.isEmpty()) {
            return clientId + "没有建立连接！";
        }

        sseCache.forEach((mapClientId, sseEmitter) -> {
            if (clientId == null || clientId.isEmpty() || mapClientId.equals(clientId)) {
                sendMsgToClientByClientId(mapClientId, msg, sseEmitter);
            }
        });

        log.info("消息发送成功！");
        return "消息发送成功！";
    }

    public void closeSseConnect(String clientId) {
        SseEmitter sseEmitter = sseCache.get(clientId);
        if (sseEmitter != null) {
            sseEmitter.complete();
            removeUser(clientId);
        }
    }

    public List<String> getClients() {
        return List.copyOf(sseCache.keySet());
    }

    private Runnable completionCallBack(String clientId) {
        return () -> {
            log.info("连接已完成：" + clientId);
            removeUser(clientId);
        };
    }

    private void sendMsgToClientByClientId(String clientId, String msg, SseEmitter sseEmitter) {
        if (sseEmitter == null) {
            log.info(String.format("推送消息失败：客户端 <%s> 没有连接，消息：%s", clientId, msg));
            return;
        }

        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event().id(clientId).data(msg, MediaType.APPLICATION_JSON);
            sseEmitter.send(event);
        } catch (IOException e) {
            retrySendingMsg(clientId, msg, sseEmitter);
        }
    }

    private void retrySendingMsg(String clientId, String msg, SseEmitter sseEmitter) {
        log.info(String.format("推送消息失败：%s，尝试重试", msg));
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL);
                sseEmitter = sseCache.get(clientId);
                if (sseEmitter == null) {
                    log.info(String.format("<%s> 重试 #%d 失败，没有连接", clientId, i + 1));
                    continue;
                }
                sseEmitter.send(SseEmitter.event().id(clientId).data(msg, MediaType.APPLICATION_JSON));
                log.info(String.format("<%s> 重试 #%d 成功，%s", clientId, i + 1, msg));
                return;
            } catch (Exception ex) {
                log.info(String.format("<%s> 重试 #%d 失败", clientId, i + 1));
            }
        }
        closeSseConnect(clientId);
    }

    private void removeUser(String clientId) {
        sseCache.remove(clientId);
        log.info("移除用户：" + clientId);
    }

    private void sseCacheChecker() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 每隔1分钟执行一次
                    TimeUnit.MINUTES.sleep(1);
//                    log.info("检查sse缓存有效性...");
                    sendMsgToClient(null, "测试");
//                    log.info("检查缓存检查完成。");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public SseManagerUtil() {
        sseCacheChecker();
    }
}
