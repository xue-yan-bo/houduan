package com.jlm.homework.socket.handler;

import com.jlm.homework.socket.netty.SocketServerHandler;
import com.jlm.homework.socket.websocket.SafeWebSocketService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HandwritingHandler implements MessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final IHandlerService handlerService;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final SocketServerHandler clientHandler;


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
                              SocketServerHandler clientHandler) {
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

    private void saveHardToRedis(String hex) {
        if (clientHandler != null) {
            clientHandler.saveHardDataToRedis(hex);
        }
    }



    @Override
    public void handle(SessionContext context, Packet packet, ResponseSender sender) {
        //log.info("Redis时间开始:"+ LocalDateTime.now());
        // todo 写入redis，核查丢包问题
        /*byte[] rawData = packet.getRawData();
        saveHardToRedis(com.jlm.homework.util.ParseTcpDataUtil.bytesToHexString(rawData));*/
//        if(1==1) return;

        //log.info("解析时间开始:"+ LocalDateTime.now());
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
                try {
                    List<StudentsWriteRecord> recordsToSave = new ArrayList<>(context.getStudentsCopybookRecords());
                    handlerService.saveStudentsCopybookRecords(
                            Long.parseLong(relation.getUserId()),
                            context.getCopybookId(),
                            context.getPageNum(),
                            recordsToSave,
                            false
                    );
                    // 只有在保存成功后才清空列表
                    context.setStudentsCopybookRecords(new ArrayList<>());
                    log.info("字帖数据保存成功，记录数: {}, copybookId: {}, userId: {}", recordsToSave.size(), context.getCopybookId(), relation.getUserId());
                } catch (Exception e) {
                    log.error("字帖数据保存失败，记录数: {}, copybookId: {}, userId: {}, 错误: {}", 
                        context.getStudentsCopybookRecords().size(), context.getCopybookId(), relation.getUserId(), e.getMessage(), e);
                    // 保存失败时不清空列表，保留数据以便下次重试
                    if (context.getStudentsCopybookRecords().size() > SessionContext.SAVE_SIZE * 2) {
                        log.warn("字帖数据列表过大，尝试分批保存");
                        try {
                            int batchSize = SessionContext.SAVE_SIZE;
                            for (int i = 0; i < context.getStudentsCopybookRecords().size(); i += batchSize) {
                                int end = Math.min(i + batchSize, context.getStudentsCopybookRecords().size());
                                List<StudentsWriteRecord> batch = new ArrayList<>(context.getStudentsCopybookRecords().subList(i, end));
                                handlerService.saveStudentsCopybookRecords(
                                        Long.parseLong(relation.getUserId()),
                                        context.getCopybookId(),
                                        context.getPageNum(),
                                        batch,
                                        false
                                );
                            }
                            context.setStudentsCopybookRecords(new ArrayList<>());
                            log.info("字帖数据分批保存成功");
                        } catch (Exception ex) {
                            log.error("字帖数据分批保存也失败: {}", ex.getMessage(), ex);
                        }
                    }
                }
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
                try {
                    List<StudentsWriteRecord> recordsToSave = new ArrayList<>(context.getStudentsWriteRecords());
                    handlerService.saveWriteRecords(
                            Long.parseLong(relation.getUserId()),
                            context.getHomeId(),
                            "1",
                            context.getPageNum(),
                            recordsToSave,
                            false
                    );
                    // 只有在保存成功后才清空列表
                    context.setStudentsWriteRecords(new ArrayList<>());
                    log.info("作业数据保存成功，记录数: {}, homeId: {}, userId: {}", recordsToSave.size(), context.getHomeId(), relation.getUserId());
                } catch (Exception e) {
                    log.error("作业数据保存失败，记录数: {}, homeId: {}, userId: {}, 错误: {}", 
                        context.getStudentsWriteRecords().size(), context.getHomeId(), relation.getUserId(), e.getMessage(), e);
                    // 保存失败时不清空列表，保留数据以便下次重试
                    if (context.getStudentsWriteRecords().size() > SessionContext.SAVE_SIZE * 2) {
                        log.warn("作业数据列表过大，尝试分批保存");
                        try {
                            int batchSize = SessionContext.SAVE_SIZE;
                            for (int i = 0; i < context.getStudentsWriteRecords().size(); i += batchSize) {
                                int end = Math.min(i + batchSize, context.getStudentsWriteRecords().size());
                                List<StudentsWriteRecord> batch = new ArrayList<>(context.getStudentsWriteRecords().subList(i, end));
                                handlerService.saveWriteRecords(
                                        Long.parseLong(relation.getUserId()),
                                        context.getHomeId(),
                                        "1",
                                        context.getPageNum(),
                                        batch,
                                        false
                                );
                            }
                            context.setStudentsWriteRecords(new ArrayList<>());
                            log.info("作业数据分批保存成功");
                        } catch (Exception ex) {
                            log.error("作业数据分批保存也失败: {}", ex.getMessage(), ex);
                        }
                    }
                }
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
                context.getStudentsWriteRecords().add(writeRecord); 
            }
        }

        synchronized (context) {
            if (context.getStudentsEmendRecords().size() >= SessionContext.SAVE_SIZE
                    && context.getHomeId() != null && context.getPageNum() != null) {
                try {
                    List<StudentsWriteRecord> recordsToSave = new ArrayList<>(context.getStudentsEmendRecords());
                    handlerService.saveWriteRecords(
                            Long.parseLong(relation.getUserId()),
                            context.getHomeId(),
                            "2",
                            context.getPageNum(),
                            recordsToSave,
                            false
                    );
                    // 只有在保存成功后才清空列表
                    context.setStudentsEmendRecords(new ArrayList<>());
                    log.info("订正数据保存成功，记录数: {}, homeId: {}, userId: {}", recordsToSave.size(), context.getHomeId(), relation.getUserId());
                } catch (Exception e) {
                    log.error("订正数据保存失败，记录数: {}, homeId: {}, userId: {}, 错误: {}", 
                        context.getStudentsEmendRecords().size(), context.getHomeId(), relation.getUserId(), e.getMessage(), e);
                    // 保存失败时不清空列表，保留数据以便下次重试
                    if (context.getStudentsEmendRecords().size() > SessionContext.SAVE_SIZE * 2) {
                        log.warn("订正数据列表过大，尝试分批保存");
                        try {
                            int batchSize = SessionContext.SAVE_SIZE;
                            for (int i = 0; i < context.getStudentsEmendRecords().size(); i += batchSize) {
                                int end = Math.min(i + batchSize, context.getStudentsEmendRecords().size());
                                List<StudentsWriteRecord> batch = new ArrayList<>(context.getStudentsEmendRecords().subList(i, end));
                                handlerService.saveWriteRecords(
                                        Long.parseLong(relation.getUserId()),
                                        context.getHomeId(),
                                        "2",
                                        context.getPageNum(),
                                        batch,
                                        false
                                );
                            }
                            context.setStudentsEmendRecords(new ArrayList<>());
                            log.info("订正数据分批保存成功");
                        } catch (Exception ex) {
                            log.error("订正数据分批保存也失败: {}", ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
    }

    private void handleFeedbackMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        // 先创建记录，确保数据不丢失
        StudentsWriteRecord writeRecord = createRecord(result);
        
        synchronized (context) {
            // 检查列表是否为空，如果为空则创建新的同步列表
            if (context.getStudentsFeedbackRecords() == null) {
                context.setStudentsFeedbackRecords(java.util.Collections.synchronizedList(new ArrayList<>()));
            }
            
            // 添加记录到同步列表
            context.getStudentsFeedbackRecords().add(writeRecord);
            List<StudentsWriteRecord> seedbackRecords = context.getStudentsFeedbackRecords();
            // 检查是否达到保存阈值且feedbackId不为空
            if (seedbackRecords.size() >= SessionContext.SAVE_SIZE) {
                // 保存反馈数据
                try {
                    // 创建一个新的列表来保存要处理的数据，避免并发修改
                    List<StudentsWriteRecord> recordsToSave = new ArrayList<>(context.getStudentsFeedbackRecords());
                    
                    // 清空原列表，避免数据重复处理
                    context.setStudentsFeedbackRecords(java.util.Collections.synchronizedList(new ArrayList<>()));
                    saveSessionContextToRedis(context);
                    // 保存数据到数据库
                    Long feedbackId = handlerService.saveFeedbackRecords(context.getFeedbackId(), Long.parseLong(relation.getUserId()), context.getFeedbackSubject(), recordsToSave);
                    context.setFeedbackId(feedbackId);
                    // 保存会话上下文到Redis
                    saveSessionContextToRedis(context);

                    log.info("反馈数据保存成功，记录数: {}, feedbackId: {}, userId: {}", recordsToSave.size(), feedbackId, relation.getUserId());
                } catch (Exception e) {
                    log.error("反馈数据保存失败，记录数: {}, feedbackId: {}, userId: {}, 错误: {}", 
                        context.getStudentsFeedbackRecords().size(), context.getFeedbackId(), relation.getUserId(), e.getMessage(), e);
                    
                    // 保存失败时，尝试分批保存
                    if (context.getStudentsFeedbackRecords().size() > SessionContext.SAVE_SIZE * 2) {
                        log.warn("反馈数据列表过大，尝试分批保存");
                        try {
                            int batchSize = SessionContext.SAVE_SIZE;
                            Long savedFeedbackId = context.getFeedbackId();
                            
                            // 分批处理数据
                            while (!context.getStudentsFeedbackRecords().isEmpty()) {
                                int batchSizeToUse = Math.min(batchSize, context.getStudentsFeedbackRecords().size());
                                List<StudentsWriteRecord> batch = new ArrayList<>();
                                
                                // 提取批次数据
                                for (int i = 0; i < batchSizeToUse && !context.getStudentsFeedbackRecords().isEmpty(); i++) {
                                    batch.add(context.getStudentsFeedbackRecords().remove(0));
                                }
                                
                                // 保存批次数据
                                savedFeedbackId = handlerService.saveFeedbackRecords(savedFeedbackId, Long.parseLong(relation.getUserId()), context.getFeedbackSubject(), batch);
                            }
                            
                            context.setFeedbackId(savedFeedbackId);
                            saveSessionContextToRedis(context);
                            log.info("反馈数据分批保存成功");
                        } catch (Exception ex) {
                            log.error("反馈数据分批保存也失败: {}", ex.getMessage(), ex);
                            // 分批保存失败时，不清空列表，保留数据以便下次重试
                        }
                    } else {
                        try {
                            // 再次尝试保存，添加异常处理
                            Long feedbackId = handlerService.saveFeedbackRecords(context.getFeedbackId(), Long.parseLong(relation.getUserId()), context.getFeedbackSubject(), context.getStudentsFeedbackRecords());
                            context.setFeedbackId(feedbackId);
                            context.setStudentsFeedbackRecords(java.util.Collections.synchronizedList(new ArrayList<>()));
                            saveSessionContextToRedis(context);
                            log.info("反馈数据保存成功");
                        } catch (Exception ex) {
                            log.error("反馈数据再次保存失败: {}", ex.getMessage(), ex);
                            // 保存失败时不清空列表，保留数据以便下次重试
                        }
                    }
                }
            }
        }
    }

    private void handleErrorTitleMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation) {
        // 先创建记录，确保数据不丢失
        StudentsWriteRecord writeRecord = createRecord(result);
        
        synchronized (context) {
            // 检查列表是否为空，如果为空则创建新的同步列表
            if (context.getUploadErrorTitleRecords() == null) {
                context.setUploadErrorTitleRecords(java.util.Collections.synchronizedList(new ArrayList<>()));
            }
            
            context.getUploadErrorTitleRecords().add(writeRecord);
            
            if (context.getUploadErrorTitleRecords().size() >= SessionContext.SAVE_SIZE) {
                try {
                    // 创建一个新的列表来保存要处理的数据，避免并发修改
                    List<StudentsWriteRecord> recordsToSave = new ArrayList<>(context.getUploadErrorTitleRecords());
                    
                    // 清空原列表，避免数据重复处理
                    context.setUploadErrorTitleRecords(java.util.Collections.synchronizedList(new ArrayList<>()));
                    
                    // 保存数据到数据库
                    Long errorTitleId = handlerService.saveErrorTitleRecords(context.getErrorTitleId(), Long.parseLong(relation.getUserId()), context.getErrorTitleSubject(), recordsToSave);
                    context.setErrorTitleId(errorTitleId);
                    log.info("错题数据保存成功，记录数: {}, errorTitleId: {}, userId: {}", recordsToSave.size(), errorTitleId, relation.getUserId());
                } catch (Exception e) {
                    log.error("错题数据保存失败，记录数: {}, errorTitleId: {}, userId: {}, 错误: {}", 
                        context.getUploadErrorTitleRecords().size(), context.getErrorTitleId(), relation.getUserId(), e.getMessage(), e);
                    
                    // 保存失败时，尝试分批保存
                    if (context.getUploadErrorTitleRecords().size() > SessionContext.SAVE_SIZE * 2) {
                        log.warn("错题数据列表过大，尝试分批保存");
                        try {
                            int batchSize = SessionContext.SAVE_SIZE;
                            Long savedErrorTitleId = context.getErrorTitleId();
                            
                            // 分批处理数据
                            while (!context.getUploadErrorTitleRecords().isEmpty()) {
                                int batchSizeToUse = Math.min(batchSize, context.getUploadErrorTitleRecords().size());
                                List<StudentsWriteRecord> batch = new ArrayList<>();
                                
                                // 提取批次数据
                                for (int i = 0; i < batchSizeToUse && !context.getUploadErrorTitleRecords().isEmpty(); i++) {
                                    batch.add(context.getUploadErrorTitleRecords().remove(0));
                                }
                                
                                // 保存批次数据
                                savedErrorTitleId = handlerService.saveErrorTitleRecords(savedErrorTitleId, Long.parseLong(relation.getUserId()), context.getErrorTitleSubject(), batch);
                            }
                            
                            context.setErrorTitleId(savedErrorTitleId);
                            log.info("错题数据分批保存成功");
                        } catch (Exception ex) {
                            log.error("错题数据分批保存也失败: {}", ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
    }

    // 课堂模式笔记记录计数器
    private final java.util.concurrent.atomic.AtomicInteger classroomModeCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    // 每处理多少条笔记记录后保存一次到Redis
    private static final int CLASSROOM_MODE_SAVE_INTERVAL = 1000;

    // 消息发送线程池，使用合理大小
    private static final java.util.concurrent.ExecutorService SEND_THREAD_POOL = java.util.concurrent.Executors.newFixedThreadPool(50);
    // 每个用户的消息队列
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.BlockingQueue<MessageTask>> USER_MESSAGE_QUEUES = new java.util.concurrent.ConcurrentHashMap<>();
    // 消息处理器映射
    private static final java.util.concurrent.ConcurrentHashMap<String, Thread> USER_MESSAGE_HANDLERS = new java.util.concurrent.ConcurrentHashMap<>();
    // 队列容量
    private static final int QUEUE_CAPACITY = 10000;

    // 启动用户消息处理线程
    private void startUserMessageHandler(String userId) {
        if (!USER_MESSAGE_HANDLERS.containsKey(userId)) {
            Thread handlerThread = new Thread(() -> {
                java.util.concurrent.BlockingQueue<MessageTask> queue = USER_MESSAGE_QUEUES.get(userId);
                if (queue == null) return;
                
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        MessageTask task = queue.take();
                        task.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error processing message task for user {}: {}", userId, e.getMessage(), e);
                    }
                }
                // 清理资源
                USER_MESSAGE_HANDLERS.remove(userId);
                USER_MESSAGE_QUEUES.remove(userId);
            }, "message-processor-" + userId);
            
            handlerThread.setDaemon(true);
            handlerThread.start();
            USER_MESSAGE_HANDLERS.put(userId, handlerThread);
        }
    }

    // 确保用户消息队列存在
    private java.util.concurrent.BlockingQueue<MessageTask> ensureUserQueue(String userId) {
        return USER_MESSAGE_QUEUES.computeIfAbsent(userId, k -> {
            java.util.concurrent.BlockingQueue<MessageTask> queue = new java.util.concurrent.LinkedBlockingQueue<>(QUEUE_CAPACITY);
            startUserMessageHandler(userId);
            return queue;
        });
    }

    private void handleClassroomMode(SessionContext context, HandwritingParseResult result, SmartDeviceUserRelation relation, ResponseSender sender) {
        result.setUserId(relation.getUserId());
        
        // 创建消息任务并加入用户队列
        String userId = relation.getUserId();
        //log.info("ws发送时间开始:"+ LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/writingData/" + userId, result);
        //log.info("ws发送时间结束:"+ LocalDateTime.now());
        /*MessageTask task = new MessageTask(userId, result, messagingTemplate);
        java.util.concurrent.BlockingQueue<MessageTask> userQueue = ensureUserQueue(userId);
        
        if (!userQueue.offer(task)) {
            // 队列已满，直接处理
            log.warn("Message queue full for user {}, processing directly", userId);
            task.execute();
        }

        // 减少保存频率，每处理CLASSROOM_MODE_SAVE_INTERVAL条记录保存一次
        if (classroomModeCounter.incrementAndGet() >= CLASSROOM_MODE_SAVE_INTERVAL) {
            // 异步保存SessionContext到Redis
            SEND_THREAD_POOL.submit(() -> {
                try {
                    saveSessionContextToRedis(context);
                } catch (Exception e) {
                    log.error("Error saving session context: {}", e.getMessage(), e);
                }
            });
            // 重置计数器
            classroomModeCounter.set(0);
        }*/
    }

    // 消息任务类
    private static class MessageTask {
        private final String userId;
        private final HandwritingParseResult result;
        private final SimpMessagingTemplate messagingTemplate;

        public MessageTask(String userId, HandwritingParseResult result, SimpMessagingTemplate messagingTemplate) {
            this.userId = userId;
            this.result = result;
            this.messagingTemplate = messagingTemplate;
        }

        public void execute() {
            int maxRetries = 2; // 减少重试次数
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
                        Thread.sleep(50 * retryCount); // 减少延迟时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
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



    private StudentsWriteRecord createRecord(HandwritingParseResult result) {
        StudentsWriteRecord writeRecord = new StudentsWriteRecord();
        writeRecord.setX(result.getX());
        writeRecord.setY(result.getY());
        writeRecord.setPressure(result.getPressure());
        writeRecord.setTimestamp(result.getTimestamp());
        return writeRecord;
    }
}
