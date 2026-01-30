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

    public SerialNumberHandler(ISmartDeviceUserRelationService smartDeviceUserRelationService,
                               SimpMessagingTemplate messagingTemplate) {
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void handle(SessionContext context, Packet packet, ResponseSender sender) {
        MacParseResult result = ParseTcpDataUtil.parseSerialNumberTcpPacket(packet.getRawData());
        log.info("Serial Number Data Parsed: {}", result);
        context.setMac(result.getMac());
        SmartDeviceUserRelation deviceUserRelation = smartDeviceUserRelationService.selectByDeviceCode(result.getMac().toString());
        if (deviceUserRelation != null) {
            deviceUserRelation.setIpAddress(context.getClientIP());
            smartDeviceUserRelationService.update(deviceUserRelation);
        } else {
            log.warn("{} Device not bound to student!", result.getMac());
            deviceUserRelation = new SmartDeviceUserRelation();
            deviceUserRelation.setIpAddress(context.getClientIP());
            deviceUserRelation.setDeviceCode(result.getMac());
            log.info("Sending bind student request /topic/bindStudent, Device Code: {}", deviceUserRelation.getDeviceCode());
            messagingTemplate.convertAndSend("/topic/bindStudent", deviceUserRelation);
        }
        
        // Echo back
        try {
            sender.sendRaw(packet.getRawData());
        } catch (IOException e) {
            log.error("Error echoing serial number packet", e);
        }
    }
}
