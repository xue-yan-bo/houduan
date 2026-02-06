package com.jlm.homework.socket.handler;

import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.HandwritingParseResult;
import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HandwritingHandler implements MessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final IHandlerService handlerService;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final com.jlm.homework.socket.ClientHandler clientHandler;

    public HandwritingHandler(SimpMessagingTemplate messagingTemplate,
                              IHandlerService handlerService,
                              ISmartDeviceUserRelationService smartDeviceUserRelationService) {
        this.messagingTemplate = messagingTemplate;
        this.handlerService = handlerService;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.clientHandler = null;
    }

    public HandwritingHandler(SimpMessagingTemplate messagingTemplate,
                              IHandlerService handlerService,
                              ISmartDeviceUserRelationService smartDeviceUserRelationService,
                              com.jlm.homework.socket.ClientHandler clientHandler) {
        this.messagingTemplate = messagingTemplate;
        this.handlerService = handlerService;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.clientHandler = clientHandler;
    }

    @Override
    public void handle(SessionContext context, Packet packet, ResponseSender sender) {
        List<HandwritingParseResult> results;
        if (packet.getType() == 0x01) {
            results = ParseTcpDataUtil.parseHandwritingTcpPackets(packet.getRawData());
        } else {
            results = ParseTcpDataUtil.parseHandwritingTcpPacketsBluetooth(packet.getRawData());
        }

        //log.debug("Handwriting data parsed, count: {}", results.size());

        for (HandwritingParseResult result : results) {
            SmartDeviceUserRelation relation = context.getRelation();
            
            if (relation != null) {
                processRecord(context, result, relation, sender);
                // 保存SessionContext到Redis
                saveSessionContextToRedis(context);
            } else {
                // Try to refresh relation from DB using IP
                relation = smartDeviceUserRelationService.selectByIpAddress(context.getClientIP());
                if (relation != null) {
                    context.setRelation(relation); // Update context
                    result.setUserId(relation.getUserId());
                    // Removed verbose server reply to reduce bandwidth
                    // sender.sendText("Server Reply: " + result.toString());
                    messagingTemplate.convertAndSend("/topic/writingData/" + relation.getUserId(), result);
                    // 保存SessionContext到Redis
                    saveSessionContextToRedis(context);
                } else {
                    log.warn("Student info not found for IP: {}", context.getClientIP());
                }
            }
        }
    }

    /**
     * 保存SessionContext到Redis
     */
    private void saveSessionContextToRedis(SessionContext context) {
        if (clientHandler != null) {
            clientHandler.saveSessionContextToRedis();
        }
    }

    private void processRecord(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation, ResponseSender sender) {
        if (context.isHomeworkFlag() && context.getCurrentMenu() != null) {
            // 作业模式
            handleHomeworkMode(context, result, relation);
        } else if (context.isEmendFlag() && context.getCurrentMenu() != null) {
            // 订正模式
            handleEmendMode(context, result, relation);
        } else if (context.isFeedbackFlag() && context.getCurrentMenu() != null) {
            // 反馈模式
            handleFeedbackMode(context, result);
        } else if (context.isErrorTitleFlag() && context.getCurrentMenu() != null) {
            // 错题模式
            handleErrorTitleMode(context, result);
        }  else if (context.isCopybookFlag()) {
            // 字帖模式
            handleCopybookMode(context, result, relation);
        }else {
            // 课堂/自由模式
            handleClassroomMode(context, result, relation, sender);
        }
    }

    private void handleCopybookMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        StudentsWriteRecord writeRecord = createRecord(result);
        if (context.getButtonTimes() != null && result.getTimestamp() < context.getButtonTimes()) {
            context.getLastList().add(writeRecord);
        } else {
            context.getStudentsCopybookRecords().add(writeRecord);
        }
        if (context.getStudentsCopybookRecords().size() >= SessionContext.SAVE_SIZE
                && context.getCopybookId() != null && context.getPageNum() != null) {
            handlerService.saveStudentsCopybookRecords(
                    Long.parseLong(relation.getUserId()),
                    context.getCopybookId(),
                    context.getPageNum(),
                    context.getStudentsCopybookRecords(),
                    false
            );
            context.setStudentsCopybookRecords(new ArrayList<>());
        }
    }

    private void handleHomeworkMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        StudentsWriteRecord writeRecord = createRecord(result);
        if (context.getButtonTimes() != null && result.getTimestamp() < context.getButtonTimes()) {
            context.getLastList().add(writeRecord);
        } else {
            context.getStudentsWriteRecords().add(writeRecord);
        }
        
        if (context.getStudentsWriteRecords().size() >= SessionContext.SAVE_SIZE 
                && context.getHomeId() != null && context.getPageNum() != null) {
            handlerService.saveWriteRecords(
                    Long.parseLong(relation.getUserId()), 
                    context.getHomeId(), 
                    "1", 
                    context.getPageNum(), 
                    context.getStudentsWriteRecords(), 
                    false
            );
            context.setStudentsWriteRecords(new ArrayList<>());
        }
    }

    private void handleEmendMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        StudentsWriteRecord writeRecord = createRecord(result);
        context.getStudentsEmendRecords().add(writeRecord);
        
        if (context.getButtonTimes() != null && result.getTimestamp() < context.getButtonTimes()) {
            context.getLastList().add(writeRecord);
        } else {
            context.getStudentsWriteRecords().add(writeRecord); // Note: Original code added to studentsWriteRecords here too? 
            // Original line 289: studentsWriteRecords.add(writeRecord); 
            // Wait, line 285 adds to studentsEmendRecords. Line 289 adds to studentsWriteRecords. 
            // This looks like double adding or mistake in original code, but I will preserve behavior.
            // Actually, looking at original code:
            // if(buttonTimes!=null&&result.getTimestamp()<buttonTimes){ lastList.add } else { studentsWriteRecords.add }
            // So it adds to studentsEmendRecords ALWAYS, and THEN conditionally to lastList OR studentsWriteRecords.
        }

        if (context.getStudentsEmendRecords().size() >= SessionContext.SAVE_SIZE 
                && context.getHomeId() != null && context.getPageNum() != null) {
            handlerService.saveWriteRecords(
                    Long.parseLong(relation.getUserId()), 
                    context.getHomeId(), 
                    "2", 
                    context.getPageNum(), 
                    context.getStudentsEmendRecords(), 
                    false
            );
            context.setStudentsEmendRecords(new ArrayList<>());
        }
    }

    private void handleFeedbackMode(SessionContext context, HandwritingParseResult result) {
        context.getStudentsFeedbackRecords().add(createRecord(result));
    }

    private void handleErrorTitleMode(SessionContext context, HandwritingParseResult result) {
        context.getUploadErrorTitleRecords().add(createRecord(result));
    }

    private void handleClassroomMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation, ResponseSender sender) {
        result.setUserId(relation.getUserId());
        // Removed verbose server reply to reduce bandwidth
        // sender.sendText("Server Reply: " + result.toString());
        // 包装消息发送操作，处理会话关闭的情况
        try {
            messagingTemplate.convertAndSend("/topic/writingData/" + relation.getUserId(), result);
        } catch (IllegalStateException e) {
            log.warn("Failed to send writing data: {}", e.getMessage());
            // 会话已关闭，跳过发送
        }
    }

    private StudentsWriteRecord createRecord(HandwritingParseResult result) {
        StudentsWriteRecord writeRecord = new StudentsWriteRecord();
        writeRecord.setX(result.getX());
        writeRecord.setY(result.getY());
        writeRecord.setPressure(result.getPressure());
        writeRecord.setTimestamp(result.getTimestamp());
        return writeRecord;
    }
}
