package com.jlm.homework.socket.handler;

import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.MacParseResult;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class SerialNumberHandler implements MessageHandler {

    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.jlm.homework.socket.ClientHandler clientHandler;

    public SerialNumberHandler(ISmartDeviceUserRelationService smartDeviceUserRelationService,
                               SimpMessagingTemplate messagingTemplate) {
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.messagingTemplate = messagingTemplate;
        this.clientHandler = null;
    }

    public SerialNumberHandler(ISmartDeviceUserRelationService smartDeviceUserRelationService,
                               SimpMessagingTemplate messagingTemplate,
                               com.jlm.homework.socket.ClientHandler clientHandler) {
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.messagingTemplate = messagingTemplate;
        this.clientHandler = clientHandler;
    }

    @Override
    public void handle(SessionContext context, Packet packet, ResponseSender sender) {
        MacParseResult result = ParseTcpDataUtil.parseSerialNumberTcpPacket(packet.getRawData());
        log.info("Serial Number Data Parsed: {}", result);
        context.setMac(result.getMac());



        // 保存SessionContext到Redis
        if (clientHandler != null) {
            clientHandler.saveSessionContextToRedis();
        }
        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.selectByDeviceCode(result.getMac().toString());

        context.setRelation(deviceUserRelation);

        if (deviceUserRelation != null) {
            deviceUserRelation.setIpAddress(context.getClientIP());
            smartDeviceUserRelationService.update(deviceUserRelation);
        } else {
            //log.warn("{} Device not bound to student!", result.getMac());
            // 使用context.getClientIP()从Redis获取数据，如果有就清除该条Redis记录
            if (clientHandler != null) {
                try {
                    String redisKey = "socket:client:" + context.getClientIP();
                    Object sessionData = clientHandler.getRedisTemplate().opsForValue().get(redisKey);
                    if (sessionData != null) {
                        clientHandler.getRedisTemplate().delete(redisKey);
                    }
                } catch (Exception e) {
                    log.warn("Failed to clear Redis data for IP: {}", context.getClientIP(), e);
                }
            }
            deviceUserRelation = new SmartDeviceUserRelation();
            deviceUserRelation.setIpAddress(context.getClientIP());
            deviceUserRelation.setDeviceCode(result.getMac());
            log.info("Sending bind student request /topic/bindStudent, Device Code: {}", deviceUserRelation.getDeviceCode());
            // 包装消息发送操作，处理会话关闭的情况
            try {
                messagingTemplate.convertAndSend("/topic/bindStudent", deviceUserRelation);
            } catch (IllegalStateException e) {
                log.warn("Failed to send bind student request: {}", e.getMessage());
                // 会话已关闭，跳过发送
            }
        }

        // Echo back
        try {
            sender.sendRaw(packet.getRawData());
        } catch (IOException e) {
            log.error("Error echoing serial number packet", e);
        }
    }
}
