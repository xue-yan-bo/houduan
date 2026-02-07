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
        // 初始化处理线程
        initProcessThread();
    }

    /**
     * 初始化处理线程池
     */
    private void initProcessThread() {
        // 启动多个处理线程，从队列中取出笔记记录并处理
        for (int i = 0; i < 50; i++) {
            processThreadPool.submit(() -> {
                while (true) {
                    try {
                        // 从队列中取出笔记记录
                        java.util.Map<String, Object> record = recordQueue.take();
                        // 处理笔记记录
                        processRecord(record);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error processing record: {}", e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 处理笔记记录
     * @param record 笔记记录
     */
    private void processRecord(java.util.Map<String, Object> record) {
        SessionContext context = (SessionContext) record.get("context");
        HandwritingParseResult result = (HandwritingParseResult) record.get("result");
        SmartDeviceUserRelation relation = (SmartDeviceUserRelation) record.get("relation");
        // 发送消息
        sendWritingDataWithRetry(relation.getUserId(), result);
        // 保存笔记记录到 SessionContext
        synchronized (context) {
            context.getStudentClassRecords().add(result);
        }
        // 定期清空 studentClassRecords，避免内存占用过高
        synchronized (context) {
            if (context.getStudentClassRecords().size() > CLASSROOM_MODE_MAX_RECORDS) {
                log.info("Clearing classroom records to avoid memory overflow: {}", context.getStudentClassRecords().size());
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

            log.debug("Handwriting data parsed, count: {}", results.size());

            for (HandwritingParseResult result : results) {
                SmartDeviceUserRelation relation = context.getRelation();
                
                if (relation != null) {
                    processRecord(context, result, relation, sender);
                } else {
                    // Try to refresh relation from DB using IP
                    relation = smartDeviceUserRelationService.selectByIpAddress(context.getClientIP());
                    if (relation != null) {
                        context.setRelation(relation); // Update context
                        // 调用processRecord方法处理笔记记录，确保通过队列处理
                        processRecord(context, result, relation, sender);
                        // 保存SessionContext到Redis
                        saveSessionContextToRedis(context);
                    } else {
                        log.warn("Student info not found for IP: {}", context.getClientIP());
                    }
                }
            }
            
            // 每处理100条笔记记录后保存一次SessionContext到Redis，减少Redis操作的频率
            if (context.getRelation() != null && context.getStudentClassRecords().size() % 100 == 0) {
                saveSessionContextToRedis(context);
            }
        } catch (Exception e) {
            log.error("Error handling handwriting data: {}", e.getMessage());
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

    // 课堂模式笔记记录计数器
    private final java.util.concurrent.atomic.AtomicInteger classroomModeCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    // 每处理多少条笔记记录后保存一次到Redis
    private static final int CLASSROOM_MODE_SAVE_INTERVAL = 5000;
    // 课堂模式笔记记录最大数量，超过后清空
    private static final int CLASSROOM_MODE_MAX_RECORDS = 10000;
    // 笔记记录队列，设置更大的容量
    private final java.util.concurrent.BlockingQueue<java.util.Map<String, Object>> recordQueue = new java.util.concurrent.LinkedBlockingQueue<>(50000);
    // 处理线程池，使用更多的线程
    private final java.util.concurrent.ExecutorService processThreadPool = java.util.concurrent.Executors.newFixedThreadPool(50);
    // 消息发送线程池，使用更大的线程池大小
    private static final java.util.concurrent.ExecutorService SEND_THREAD_POOL = java.util.concurrent.Executors.newFixedThreadPool(500);

    private void handleClassroomMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation, ResponseSender sender) {
        result.setUserId(relation.getUserId());
        // 将笔记记录添加到队列中，由处理线程处理
        try {
            java.util.Map<String, Object> record = new java.util.HashMap<>();
            record.put("context", context);
            record.put("result", result);
            record.put("relation", relation);
            // 使用带超时参数的offer方法，当队列已满时，会等待一段时间
            boolean added = recordQueue.offer(record, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!added) {
                log.warn("Queue is full, processing record directly");
                // 如果队列已满，直接处理笔记记录
                sendWritingDataWithRetry(relation.getUserId(), result);
                synchronized (context) {
                    context.getStudentClassRecords().add(result);
                }
            }
        } catch (Exception e) {
            log.error("Error adding record to queue: {}", e.getMessage());
            // 如果队列添加失败，直接处理笔记记录
            sendWritingDataWithRetry(relation.getUserId(), result);
            synchronized (context) {
                context.getStudentClassRecords().add(result);
            }
        }
    }

    /**
     * 发送书写数据，带重试机制
     * @param userId 用户ID
     * @param result 书写数据
     */
    private void sendWritingDataWithRetry(String userId, HandwritingParseResult result) {
        // 使用线程池发送消息，避免阻塞处理线程
        SEND_THREAD_POOL.submit(() -> {
            int maxRetries = 10;
            int retryDelay = 500; // 毫秒
            for (int i = 0; i < maxRetries; i++) {
                try {
                    messagingTemplate.convertAndSend("/topic/writingData/" + userId, result);
                    return; // 发送成功，直接返回
                } catch (Exception e) {
                    log.warn("Failed to send writing data (attempt {} of {}): {}", i + 1, maxRetries, e.getMessage());
                    if (i < maxRetries - 1) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            // 所有重试都失败，记录错误
            log.error("All attempts to send writing data failed for user: {}", userId);
        });
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
