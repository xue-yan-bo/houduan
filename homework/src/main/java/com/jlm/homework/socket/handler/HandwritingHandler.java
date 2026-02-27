package com.jlm.homework.socket.handler;

import com.jlm.homework.socket.websocket.WebSocketSessionManager;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.ClientHandler;
import com.jlm.homework.socket.HandwritingParseResult;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HandwritingHandler implements MessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final IHandlerService handlerService;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final ClientHandler clientHandler;


    public HandwritingHandler(SimpMessagingTemplate messagingTemplate,
                              IHandlerService handlerService,
                              ISmartDeviceUserRelationService smartDeviceUserRelationService, WebSocketSessionManager sessionManager) {
        this.messagingTemplate = messagingTemplate;
        this.handlerService = handlerService;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.clientHandler = null;
    }

    public HandwritingHandler(SimpMessagingTemplate messagingTemplate,
                              IHandlerService handlerService,
                              ISmartDeviceUserRelationService smartDeviceUserRelationService,
                              ClientHandler clientHandler) {
        this.messagingTemplate = messagingTemplate;
        this.handlerService = handlerService;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.clientHandler = clientHandler;
    }

    /**
     * 保存SessionContext到Redis
     */
    private void saveSessionContextToRedis(SessionContext context) {
        if (clientHandler != null) {
            clientHandler.saveSessionContextToRedis();
        }
    }

    @Override
    public void handle(SessionContext context, Packet packet, ResponseSender sender) {
        List<HandwritingParseResult> results;
        try {
            if (packet.getType() == 0x01) {
                results = ParseTcpDataUtil.parseHandwritingTcpPackets(packet.getRawData());
            } else {
                results = ParseTcpDataUtil.parseHandwritingTcpPacketsBluetooth(packet.getRawData());
            }

            //log.info("Handwriting data parsed, count: {}", results.size());

            for (HandwritingParseResult result : results) {
                SmartDeviceUserRelation relation = context.getRelation();
                
                if (relation != null) {
                    //log.info("Processing handwriting data for user: {}", relation.getUserId());
                    processRecord(context, result, relation, sender);
                } else {
                    // Try to refresh relation from DB using IP
                    relation = smartDeviceUserRelationService.selectByIpAddress(context.getClientIP());
                    // 如果通过IP查不到，尝试通过设备序列号（MAC）查询
                    // 这解决了设备先连接再通过前端API绑定学生时，ipAddress未写入数据库的问题
                    if (relation == null && context.getMac() != null && !context.getMac().isEmpty()) {
                        relation = smartDeviceUserRelationService.selectByDeviceCode(context.getMac());
                        if (relation != null) {
                            // 同时更新数据库中的ipAddress，方便后续查询
                            relation.setIpAddress(context.getClientIP());
                            smartDeviceUserRelationService.update(relation);
                        }
                    }
                    if (relation != null) {
                        context.setRelation(relation); // Update context
                        //log.info("Processing handwriting data for user: {}", relation.getUserId());
                        // 调用processRecord方法处理笔记记录，确保通过队列处理
                        processRecord(context, result, relation, sender);
                        // 保存SessionContext到Redis
                        saveSessionContextToRedis(context);
                    } else {
                        // 即使没有找到relation，也保存手写数据到SessionContext
                        // 这样当后续绑定学生时，能够恢复这些数据
                        //log.warn("Student info not found for IP: {}, saving handwriting data to session context", context.getClientIP());
                        synchronized (context) {
                            context.getStudentClassRecords().add(result);
                            //log.warn("Added handwriting data to session context, current size: {}", context.getStudentClassRecords().size());
                        }
                    }
                }
            }
            
            // 每处理100条笔记记录后保存一次SessionContext到Redis，减少Redis操作的频率
            if (context.getRelation() != null && context.getStudentClassRecords().size() % 100 == 0) {
                saveSessionContextToRedis(context);
            }
        } catch (Exception e) {
            log.error("Error handling handwriting data: {}", e.getMessage(), e);
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
            handleFeedbackMode(context, result, relation);
        } else if (context.isErrorTitleFlag() && context.getCurrentMenu() != null) {
            // 错题模式
            handleErrorTitleMode(context, result, relation);
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
        synchronized (context) {
            if (context.getButtonTimes() != null && result.getTimestamp() < context.getButtonTimes()) {
                context.getLastList().add(writeRecord);
            } else {
                context.getStudentsCopybookRecords().add(writeRecord);
            }
        }
        synchronized (context) {
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
    }

    private void handleHomeworkMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        StudentsWriteRecord writeRecord = createRecord(result);
        synchronized (context) {
            if (context.getButtonTimes() != null && result.getTimestamp() < context.getButtonTimes()) {
                context.getLastList().add(writeRecord);
            } else {
                context.getStudentsWriteRecords().add(writeRecord);
            }
        }

        synchronized (context) {
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
    }

    private void handleEmendMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        StudentsWriteRecord writeRecord = createRecord(result);
        synchronized (context) {
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
        }

        synchronized (context) {
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
    }

    private void handleFeedbackMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        synchronized (context) {
            // 使用带大小限制的添加方法，防止内存溢出
            context.getStudentsFeedbackRecords().add(createRecord(result));
            if (context.getStudentsFeedbackRecords().size() >= SessionContext.SAVE_SIZE&&context.getFeedbackId()!=null) {
                // 保存反馈数据但不更新feedbackId
                handlerService.saveFeedbackRecords(context.getFeedbackId(),Long.parseLong(relation.getUserId()), context.getFeedbackSubject(), context.getStudentsFeedbackRecords());
                context.setStudentsFeedbackRecords(new ArrayList<>());
                saveSessionContextToRedis(context);
            }
        }
    }

    private void handleErrorTitleMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        synchronized (context) {
            context.getUploadErrorTitleRecords().add(createRecord(result));
            if (context.getUploadErrorTitleRecords().size() >= SessionContext.SAVE_SIZE) {
                Long errorTitleId = handlerService.saveErrorTitleRecords(context.getErrorTitleId(),Long.parseLong(relation.getUserId()), context.getErrorTitleSubject(), context.getUploadErrorTitleRecords());
                context.setErrorTitleId(errorTitleId);
                context.setUploadErrorTitleRecords(new ArrayList<>());
            }
        }
    }

    // 课堂模式笔记记录计数器
    private final java.util.concurrent.atomic.AtomicInteger classroomModeCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    // 每处理多少条笔记记录后保存一次到Redis
    private static final int CLASSROOM_MODE_SAVE_INTERVAL = 1000;
    // 课堂模式笔记记录最大数量，超过后清空
    private static final int CLASSROOM_MODE_MAX_RECORDS = 4000;
    // 消息发送线程池，使用更大的线程池大小
    private static final java.util.concurrent.ExecutorService SEND_THREAD_POOL = java.util.concurrent.Executors.newFixedThreadPool(1000);

    private void handleClassroomMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation, ResponseSender sender) {
        result.setUserId(relation.getUserId());
        // 创建result对象的深拷贝，避免异步发送时数据被修改
        HandwritingParseResult resultCopy = deepCopyResult(result);
        // 直接发送消息，避免队列积压
        //log.info("Sending writing data for user: {}, X: {}, Y: {}, Pressure: {}, Timestamp: {}",
        //        relation.getUserId(), result.getX(), result.getY(), result.getPressure(), result.getTimestamp());
        sendWritingDataWithRetry(relation.getUserId(), resultCopy);

        // 优化同步块，减少锁竞争
        // 合并两个同步块为一个，减少锁的获取和释放次数
        synchronized (context) {
            context.getStudentClassRecords().add(result);
            //log.info("Added writing data to session context, current size: {}", context.getStudentClassRecords().size());

            // 定期清空 studentClassRecords，避免内存占用过高
            if (context.getStudentClassRecords().size() > CLASSROOM_MODE_MAX_RECORDS) {
                //log.info("Clearing classroom records to avoid memory overflow: {}", context.getStudentClassRecords().size());
                context.getStudentClassRecords().clear();
            }
        }

        // 减少保存频率，每处理CLASSROOM_MODE_SAVE_INTERVAL条记录保存一次
        if (classroomModeCounter.incrementAndGet() >= CLASSROOM_MODE_SAVE_INTERVAL) {
            // 保存SessionContext到Redis
            saveSessionContextToRedis(context);
            // 重置计数器
            classroomModeCounter.set(0);
        }
    }

    /**
     * 创建HandwritingParseResult对象的深拷贝
     * @param original 原始对象
     * @return 拷贝后的对象
     */
    private HandwritingParseResult deepCopyResult(HandwritingParseResult original) {
        HandwritingParseResult copy = new HandwritingParseResult();
        copy.setHeader(original.getHeader() != null ? original.getHeader().clone() : null);
        copy.setLength(original.getLength());
        copy.setType(original.getType());
        copy.setX(original.getX());
        copy.setY(original.getY());
        copy.setPressure(original.getPressure());
        copy.setTimestamp(original.getTimestamp());
        copy.setChecksum(original.getChecksum());
        copy.setChecksumValid(original.isChecksumValid());
        copy.setUserId(original.getUserId());
        return copy;
    }

    /**
     * 发送书写数据，带重试机制
     * @param userId 用户ID
     * @param result 书写数据
     */
    private void sendWritingDataWithRetry(String userId, HandwritingParseResult result) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                messagingTemplate.convertAndSend("/topic/writingData/" + userId, result);
                return;
            } catch (Exception e) {
                // 检查是否是会话关闭相关的异常
                if (e instanceof IllegalStateException && e.getMessage().contains("session is closed")) {
                    log.debug("WebSocket session closed for user {}, skipping message", userId);
                    return;
                }
                
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.warn("Failed to send writing data after {} retries: {}", maxRetries, e.getMessage());
                    return;
                }

                // 短暂延迟后重试
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
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
