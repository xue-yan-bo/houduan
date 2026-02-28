package com.jlm.homework.socket;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.handler.*;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.socket.protocol.PacketDecoder;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Refactored ClientHandler
 */
@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final IHandlerService handlerService;

    private SessionContext sessionContext;
    private ResponseSender responseSender;
    private byte[] preReadBytes;
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private String redisKey;
    private java.io.BufferedInputStream proxyBufferedInputStream;
    
    // Handlers
    private final Map<Byte, MessageHandler> handlers = new HashMap<>();
    
    // 处理线程池，使用固定大小的线程池，增加线程数以提高并发处理能力
    private static final ExecutorService PROCESS_THREAD_POOL = new ThreadPoolExecutor(
            100, // 核心线程数
            500, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), // 有界队列，避免内存溢出
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用者线程执行任务
    );
    
    // Heartbeat
    private ScheduledExecutorService heartbeatScheduler;
    private static final byte HEARTBEAT_TYPE = 0x05;
    private static final long HEARTBEAT_INTERVAL = 4; // 减少心跳间隔到2秒，保持连接更活跃
    private static final int MAX_HEARTBEAT_FAILURES = 5; // 增加允许的心跳失败次数到5次
    private int heartbeatFailureCount = 0; // 心跳失败计数器
    private volatile boolean isHeartbeatActive = true; // 心跳是否活跃
    
    // 网络异常恢复
    private long lastNetworkExceptionTime = 0; // 上次网络异常时间
    private int networkExceptionCount = 0; // 网络异常计数
    private static final long NETWORK_RECOVERY_THRESHOLD = 30000; // 网络恢复阈值，30秒
    private static final int MAX_NETWORK_EXCEPTIONS = 10; // 最大网络异常次数

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IHandlerService handlerService, SessionContext sessionContext, byte[] preReadBytes,
                         org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, String redisKey) {
        this(socket, messagingTemplate, smartDeviceUserRelationService, handlerService, sessionContext, preReadBytes, redisTemplate, redisKey, null);
    }

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IHandlerService handlerService, SessionContext sessionContext, byte[] preReadBytes,
                         org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, String redisKey,
                         java.io.BufferedInputStream proxyBufferedInputStream) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.handlerService = handlerService;
        this.sessionContext = sessionContext;
        this.preReadBytes = preReadBytes;
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
        this.proxyBufferedInputStream = proxyBufferedInputStream;
    }

    private void initHandlers() {
        // Dependencies for handlers
        HandwritingHandler handwritingHandler = new HandwritingHandler(messagingTemplate, handlerService, smartDeviceUserRelationService,this);
        ButtonHandler buttonHandler = new ButtonHandler(messagingTemplate, handlerService, responseSender, smartDeviceUserRelationService, this);
        SerialNumberHandler serialNumberHandler = new SerialNumberHandler(smartDeviceUserRelationService, messagingTemplate, this);

        handlers.put((byte) 0x01, handwritingHandler); // Handwriting
        handlers.put((byte) 0x81, handwritingHandler); // Handwriting Bluetooth
        handlers.put((byte) 0x02, buttonHandler);      // Button
        handlers.put((byte) 0x82, buttonHandler);      // Button Bluetooth
        handlers.put((byte) 0x03, serialNumberHandler); // Serial Number
    }

    @Override
    public void run() {
        InputStream in = null;
        OutputStream out = null;
        PacketDecoder decoder = null;
        java.io.BufferedInputStream bufferedInputStream = null;
        try {
            // 获取输入输出流（不使用try-with-resources，避免自动关闭）
            // 复用 SocketService 中 parseProxyHeader 创建的 BufferedInputStream，避免重复包装导致数据丢失
            if (proxyBufferedInputStream != null) {
                bufferedInputStream = proxyBufferedInputStream;
            } else {
                in = clientSocket.getInputStream();
                bufferedInputStream = new java.io.BufferedInputStream(in, 1024);
            }
            out = clientSocket.getOutputStream();
            
            // 保持连接活跃，去掉超时限制
            clientSocket.setKeepAlive(true);
            // 设置TCP连接超时时间为0（无限），避免因超时而断开
            clientSocket.setSoTimeout(0);
            // 启用TCP_NODELAY，减少延迟
            clientSocket.setTcpNoDelay(true);
            // 增加接收缓冲区大小，减少数据包丢失
            clientSocket.setReceiveBufferSize(128 * 1024);
            // 增加发送缓冲区大小，提高发送效率
            clientSocket.setSendBufferSize(128 * 1024);
            // 设置TCP性能偏好，优先考虑延迟
            clientSocket.setPerformancePreferences(1, 10, 1);
            // 设置SO_LINGER为0，避免连接关闭时阻塞
            clientSocket.setSoLinger(false, 0);
            
            // 优化TCP keepalive参数，保障长时间连接
            // 注意：Socket类已经通过setKeepAlive(true)启用了keepalive
            // 具体的keepalive参数（如空闲时间、间隔、次数）在不同平台上设置方式不同
            // 我们已经启用了keepalive，这将有助于保持连接活跃
            //log.debug("TCP keepalive is enabled");
            this.responseSender = new ResponseSender(out);
            initHandlers();

            // 1. Already got real address from SocketService, use it directly
            logClientConnection();

            // Load initial relation
            SmartDeviceUserRelation relation = smartDeviceUserRelationService.selectByIpAddress(sessionContext.getClientIP());
            if(relation==null&&StringUtils.isNotBlank(sessionContext.getMac())){
                relation = smartDeviceUserRelationService.selectByDeviceCode(sessionContext.getMac());
            }
            sessionContext.setRelation(relation);

            // 2. Setup Packet Decoder
            decoder = new PacketDecoder(bufferedInputStream);
            // Push pre-read bytes if any
            if (preReadBytes != null && preReadBytes.length > 0) {
                decoder.pushPreReadBytes(preReadBytes);
            }

            // 3. Start Heartbeat
            initHeartbeatScheduler(out);

            // 4. Main Loop - 保持运行，不自动关闭
            int connectionCheckCount = 0;
            int networkFluctuationCount = 0;
            long lastNetworkFluctuationTime = 0;
            while (true) {
                try {
                    // 检查Socket是否仍然连接
                    if (clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected()) {
                        //log.info("Socket is not connected, exiting loop. Client: {}", sessionContext.getClientIP());
                        break;
                    }
                    
                    // 检查心跳是否活跃
                    if (!isHeartbeatActive) {
                        //log.warn("Heartbeat is not active, checking connection status. Client: {}", sessionContext.getClientIP());
                        // 尝试恢复心跳
                        try {
                            // 检查输出流是否可用
                            if (out != null) {
                                // 尝试发送一个测试数据包
                                out.write(new byte[]{0x55, 0x56, 0x02, HEARTBEAT_TYPE, 0x01, 0x04});
                                out.flush();
                                //log.info("Test packet sent successfully, reactivating heartbeat. Client: {}", sessionContext.getClientIP());
                                isHeartbeatActive = true;
                                heartbeatFailureCount = 0;
                                networkFluctuationCount = 0; // 重置网络波动计数
                            } else {
                                //log.warn("OutputStream is null, cannot recover heartbeat. Client: {}", sessionContext.getClientIP());
                                break;
                            }
                        } catch (IOException e) {
                            //log.warn("Failed to send test packet: {}, closing connection. Client: {}", e.getMessage(), sessionContext.getClientIP());
                            break;
                        }
                    }
                    
                    // 定期检查连接状态（每100次循环检查一次）
                    connectionCheckCount++;
                    if (connectionCheckCount >= 100) {
                        connectionCheckCount = 0;
                        try {
                            // 检查Socket连接状态
                            if (clientSocket.isClosed() || !clientSocket.isConnected()) {
                                //log.warn("Connection check failed, socket is closed or not connected. Client: {}", sessionContext.getClientIP());
                                break;
                            }
                            
                            // 检查网络延迟
                            long startTime = System.currentTimeMillis();
                            // 发送一个小的测试数据包
                            out.write(new byte[]{0x55, 0x56, 0x02, HEARTBEAT_TYPE, 0x01, 0x04});
                            out.flush();
                            long endTime = System.currentTimeMillis();
                            long networkDelay = endTime - startTime;
                            
                            if (networkDelay > 1000) {
                                networkFluctuationCount++;
                                lastNetworkFluctuationTime = System.currentTimeMillis();
                                //log.warn("Network delay detected: {}ms, fluctuation count: {}. Client: {}",
                                //        networkDelay, networkFluctuationCount, sessionContext.getClientIP());
                                
                                // 如果网络延迟严重，调整心跳间隔
                                if (networkFluctuationCount >= 3) {
                                    //log.warn("Persistent network fluctuation detected, increasing heartbeat interval. Client: {}", sessionContext.getClientIP());
                                    // 这里可以动态调整心跳间隔
                                }
                            } else {
                                // 网络正常，重置波动计数
                                if (networkFluctuationCount > 0 && System.currentTimeMillis() - lastNetworkFluctuationTime > 30000) {
                                    networkFluctuationCount = 0;
                                    //log.info("Network recovered, resetting fluctuation count. Client: {}", sessionContext.getClientIP());
                                }
                            }
                        } catch (Exception e) {
                            String errorMsg = e.getMessage();
                            // 检查是否是连接断开错误
                            if (errorMsg != null && (errorMsg.contains("断开的管道") || errorMsg.contains("Broken pipe") || 
                                    errorMsg.contains("Connection reset") || errorMsg.contains("Socket closed"))) {
                                //log.warn("Connection check detected disconnection: {}. Client: {}", errorMsg, sessionContext.getClientIP());
                                isHeartbeatActive = false;
                                break;
                            }
                            // 其他错误，记录但继续
                            //log.warn("Connection check error: {}. Client: {}", errorMsg, sessionContext.getClientIP());
                        }
                    }
                    
                    // 不再设置Socket超时，因为已经在初始化时设置为0（无限）
                    // 这样可以避免因超时而断开连接
                    
                    Packet packet = decoder.readNextPacket();
                    if (packet == null) {
                        // Socket仍然连接，可能是暂时没有数据，短暂休眠后继续
                        // 减少sleep时间，从10ms减少到1ms，提高响应速度
                        Thread.sleep(1);
                        continue;
                    }
                    
                    // 处理蓝牙数据标记
                    if (packet.isBluetooth()) {
                        handleBluetoothPreProcess(packet);
                    } else {
                        sessionContext.setBluetooth(false);
                    }

                    // Dispatch到对应的处理器
                    MessageHandler handler = handlers.get(packet.getType());
                    if (handler != null) {
                        // 使用固定大小的线程池来处理数据包，提高并发处理能力
                        final Packet finalPacket = packet; // 避免lambda变量捕获问题
                        PROCESS_THREAD_POOL.submit(() -> {
                            try {
                                handler.handle(sessionContext, finalPacket, responseSender);
                            } catch (Exception e) {
                                log.error("Error handling packet type {} for client: {}", finalPacket.getType(), sessionContext.getClientIP(), e);
                            }
                        });
                    } else {
                        //log.warn("Unknown packet type: {} for client: {}", String.format("0x%02X", packet.getType()), sessionContext.getClientIP());
                    }

                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    //log.info("ClientHandler thread interrupted, exiting...");
                    isHeartbeatActive = false;
                    break;
                } catch (IOException e) {
                    // IO异常可能表示连接断开，但也可能是临时错误
                    String errorMsg = e.getMessage();
                    //log.warn("IO exception: {}", errorMsg);
                    
                    // 检查是否是"断开的管道"错误
                    if (errorMsg != null && (errorMsg.contains("断开的管道") || errorMsg.contains("Broken pipe") || errorMsg.contains("Connection reset"))) {
                        //log.warn("Connection reset or broken pipe detected, closing connection. Client: {}", sessionContext.getClientIP());
                        isHeartbeatActive = false;
                        break;
                    }
                    
                    // 检查Socket状态，只有在真正断开时才退出
                    if (clientSocket == null || clientSocket.isClosed()) {
                        //log.info("Socket is closed, exiting loop. Client: {}", sessionContext.getClientIP());
                        isHeartbeatActive = false;
                        break;
                    }
                    
                    // 处理网络异常
                    handleNetworkException(e);
                    
                    // 如果Socket仍然连接，可能是临时错误，短暂休眠后继续
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isHeartbeatActive = false;
                        break;
                    }
                    
                    // 继续循环，不要立即退出
                    continue;
                } catch (Exception e) {
                    // 其他异常，记录并继续
                    log.error("ClientHandler exception, continuing...: {}", e.getMessage(), e);
                    
                    // 处理网络相关异常
                    if (e instanceof java.net.SocketException || e instanceof java.net.SocketTimeoutException) {
                        handleNetworkException(e);
                    }
                    
                    // 短暂休眠，避免异常风暴
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isHeartbeatActive = false;
                        break;
                    }
                }
            }

        } catch (IOException e) {
            log.info("Client handler exception: {}", e.getMessage());
        } finally {
            // 保存未写入数据库的笔记记录
            saveUnsavedNotes();
            // 关闭资源
            cancelHeartbeat();
            isHeartbeatActive = false; // 确保心跳调度器不会继续运行
            
            try {
                if (in != null) {
                    in.close();
                    in = null; // 释放引用
                }
                if (out != null) {
                    out.close();
                    out = null; // 释放引用
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    //clientSocket = null; // 释放引用
                }
            } catch (IOException e) {
                log.info("Error closing socket: {}", e.getMessage());
            }
            
            // 清理其他资源，避免内存泄漏
            if (handlers != null) {
                handlers.clear(); // 清理处理器映射
            }
            
            // 释放引用
            preReadBytes = null;
            
            //log.info("Client connection closed: {}", sessionContext.getClientIP());
        }
    }
    /**
     * 保存未写入数据库的笔记记录
     */
    private void saveUnsavedNotes() {
        SmartDeviceUserRelation relation = sessionContext.getRelation();
        if (relation != null && StringUtils.isNotEmpty(relation.getUserId())) {
            try {
                Long userId = Long.parseLong(relation.getUserId());
                
                // 保存作业模式的未写入记录
                if (!sessionContext.getStudentsWriteRecords().isEmpty() && sessionContext.getHomeId() != null && sessionContext.getPageNum() != null) {
                    handlerService.saveWriteRecords(userId, sessionContext.getHomeId(), "1", sessionContext.getPageNum(), sessionContext.getStudentsWriteRecords(), false);
                    //log.info("Saved unsaved homework notes for user {}: {}", userId, sessionContext.getStudentsWriteRecords().size());
                }
                
                // 保存订正模式的未写入记录
                if (!sessionContext.getStudentsEmendRecords().isEmpty() && sessionContext.getHomeId() != null && sessionContext.getPageNum() != null) {
                    handlerService.saveWriteRecords(userId, sessionContext.getHomeId(), "2", sessionContext.getPageNum(), sessionContext.getStudentsEmendRecords(), false);
                    //log.info("Saved unsaved emend notes for user {}: {}", userId, sessionContext.getStudentsEmendRecords().size());
                }
                
                // 保存字帖模式的未写入记录
                if (!sessionContext.getStudentsCopybookRecords().isEmpty() && sessionContext.getCopybookId() != null && sessionContext.getPageNum() != null) {
                    handlerService.saveStudentsCopybookRecords(userId, sessionContext.getCopybookId(), sessionContext.getPageNum(), sessionContext.getStudentsCopybookRecords(), false);
                    //log.info("Saved unsaved copybook notes for user {}: {}", userId, sessionContext.getStudentsCopybookRecords().size());
                }
                
                // 保存反馈模式的未写入记录
                if (!sessionContext.getStudentsFeedbackRecords().isEmpty() && sessionContext.getCurrentMenu() != null) {
                    MenuItemT itemT = sessionContext.getCurrentMenu().getPItems().get(sessionContext.getCurrentMenu().getSelectItem());
                    String name = itemT.getDesc();
                    handlerService.saveFeedbackRecords(sessionContext.getFeedbackId(),userId, name, sessionContext.getStudentsFeedbackRecords());
                    //log.info("Saved unsaved feedback notes for user {}: {}", userId, sessionContext.getStudentsFeedbackRecords().size());
                }
                
                // 保存错题模式的未写入记录
                if (!sessionContext.getUploadErrorTitleRecords().isEmpty() && sessionContext.getCurrentMenu() != null) {
                    MenuItemT itemT = sessionContext.getCurrentMenu().getPItems().get(sessionContext.getCurrentMenu().getSelectItem());
                    String name = itemT.getDesc();
                    handlerService.saveErrorTitleRecords(sessionContext.getErrorTitleId(),userId, name, sessionContext.getUploadErrorTitleRecords());
                    //log.info("Saved unsaved error title notes for user {}: {}", userId, sessionContext.getUploadErrorTitleRecords().size());
                }
                
                // 保存 lastList 中的记录
                if (!sessionContext.getLastList().isEmpty()) {
                    if (sessionContext.getCopybookId() != null && sessionContext.getPageNum() != null && sessionContext.getPageNum() > 1) {
                        handlerService.saveStudentsCopybookRecords(userId, sessionContext.getCopybookId(), sessionContext.getPageNum() - 1, sessionContext.getLastList(), false);
                        //log.info("Saved unsaved lastList notes for user {}: {}", userId, sessionContext.getLastList().size());
                    } else if (sessionContext.getHomeId() != null && sessionContext.getPageNum() != null && sessionContext.getPageNum() > 1) {
                        handlerService.saveWriteRecords(userId, sessionContext.getHomeId(), "1", sessionContext.getPageNum() - 1, sessionContext.getLastList(), false);
                        //log.info("Saved unsaved lastList notes for user {}: {}", userId, sessionContext.getLastList().size());
                    }
                }
                
                // 保存课堂模式的未写入记录（清空studentClassRecords，避免内存占用过高）
                if (!sessionContext.getStudentClassRecords().isEmpty()) {
                    //log.info("Saved unsaved classroom notes for user {}: {}", userId, sessionContext.getStudentClassRecords().size());
                    // 清空studentClassRecords，避免内存占用过高
                    sessionContext.getStudentClassRecords().clear();
                    // 保存修改后的SessionContext回Redis
                    saveSessionContextToRedis();
                }
                
            } catch (Exception e) {
                //log.warn("Failed to save unsaved notes: {}", e.getMessage());
            }
        }
    }

    private void handleBluetoothPreProcess(Packet packet) {
        sessionContext.setBluetooth(true);
        // MAC is already extracted in Packet (if I implemented it right)
        // But Packet.mac is byte array.
        // Logic:
        byte[] macBytes = Arrays.copyOfRange(packet.getRawData(), 4, 10);
        String mac = ParseTcpDataUtil.bytesToHexString(macBytes);
        sessionContext.setMac(mac);
        // 保存修改后的SessionContext回Redis
        saveSessionContextToRedis();

        // Optimization: Skip DB query if relation is already cached and matches MAC
        if (sessionContext.getRelation() != null &&
                String.valueOf(mac).equals(sessionContext.getRelation().getDeviceCode())) {
            return;
        }

        SmartDeviceUserRelation relation = smartDeviceUserRelationService.selectByDeviceCode(mac.toString());
        if (relation == null) {
            //System.out.println(mac + "设备还未绑定学生，请检查！");
            SmartDeviceUserRelation newRelation = new SmartDeviceUserRelation();
            newRelation.setIpAddress(sessionContext.getClientIP());
            newRelation.setDeviceCode(mac.toString());
            // 包装消息发送操作，处理会话关闭的情况
            try {
                messagingTemplate.convertAndSend("/topic/bindStudent", newRelation);
            } catch (IllegalStateException e) {
                //log.warn("Failed to send bind student request: {}", e.getMessage());
                // 会话已关闭，跳过发送
            }
        } else if (StringUtils.isEmpty(relation.getIpAddress())) {
            relation.setIpAddress(sessionContext.getClientIP());
            smartDeviceUserRelationService.update(relation);
            sessionContext.setRelation(relation); // Update context relation
            // 保存修改后的SessionContext回Redis
            saveSessionContextToRedis();
        } else {
            // Ensure context has relation
            if (sessionContext.getRelation() == null) {
                sessionContext.setRelation(relation);
                // 保存修改后的SessionContext回Redis
                saveSessionContextToRedis();
            }
        }
    }

    /**
     * 保存SessionContext到Redis
     */
    public void saveSessionContextToRedis() {
        if (redisTemplate != null && redisKey != null) {
            // 异步保存SessionContext到Redis，避免阻塞当前线程
            new Thread(() -> {
                try {
                    // 直接保存当前的SessionContext，不进行合并操作，减少网络I/O
                    redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                    // 减少日志输出，避免影响性能
                    // log.info("Saved session context to Redis for client: {}", sessionContext.getClientIP());
                } catch (Exception e) {
                    // 减少日志输出，避免影响性能
                    // log.warn("Failed to save session context to Redis: {}", e.getMessage());
                }
            }).start();
        }
    }

    /**
     * 获取RedisTemplate
     * @return RedisTemplate实例
     */
    public org.springframework.data.redis.core.RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    private void logClientConnection() {
        Map<String, Object> data = new HashMap<>();
        data.put("clientIP", sessionContext.getClientIP());
        data.put("clientPort", sessionContext.getClientPort());
        writeDebugLog("C", "ClientHandler:connect", "Client Connected", data);
    }

    private void writeDebugLog(String id, String loc, String msg, Map<String, Object> data) {
         // ... reuse original log logic or simplified ...
         log.info("[PROXY-DEBUG] [{}] {} | {} | {}", id, loc, msg, data);
    }

    // Heartbeat Logic
    private void initHeartbeatScheduler(OutputStream out) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                // 检查心跳是否活跃
                if (!isHeartbeatActive) {
                    //log.info("Heartbeat is not active, stopping heartbeat scheduler");
                    cancelHeartbeat();
                    return;
                }
                
                // 先检查Socket连接状态
                if (clientSocket == null || clientSocket.isClosed()) {
                    //log.info("Socket is closed, stopping heartbeat");
                    isHeartbeatActive = false;
                    cancelHeartbeat();
                    return;
                }
                
                // 检查输出流是否可用
                if (out == null) {
                    //log.warn("OutputStream is null, stopping heartbeat");
                    isHeartbeatActive = false;
                    cancelHeartbeat();
                    return;
                }
                
                // 尝试发送心跳
                sendHeartbeat(out);
                
                // 心跳发送成功，重置失败计数
                heartbeatFailureCount = 0;
                
            } catch (IOException e) {
                // 心跳失败，增加失败计数
                heartbeatFailureCount++;
                //log.warn("Heartbeat failed (attempt {} of {}): {}", heartbeatFailureCount, MAX_HEARTBEAT_FAILURES, e.getMessage());
                
                // 如果是"断开的管道"错误，直接关闭连接
                if (e.getMessage() != null && (e.getMessage().contains("断开的管道") || e.getMessage().contains("Broken pipe"))) {
                    //log.info("Broken pipe detected, closing connection immediately");
                    isHeartbeatActive = false;
                    cancelHeartbeat();
                    // 关闭Socket
                    try {
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                    } catch (IOException ex) {
                        // 忽略关闭异常
                    }
                    return;
                }
                
                // 如果心跳失败次数超过阈值，关闭连接
                if (heartbeatFailureCount >= MAX_HEARTBEAT_FAILURES) {
                    log.error("Heartbeat failed {} times, closing connection", MAX_HEARTBEAT_FAILURES);
                    isHeartbeatActive = false;
                    cancelHeartbeat();
                    // 关闭Socket
                    try {
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                    } catch (IOException ex) {
                        // 忽略关闭异常
                    }
                }
            } catch (Exception e) {
                // 其他异常，记录并继续
                log.error("Unexpected error in heartbeat: {}", e.getMessage(), e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    private void sendHeartbeat(OutputStream out) throws IOException {
        // 应用层心跳数据
        byte heartbeatData = 0x01;
        // 添加时间戳，确保每次心跳包内容不同
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFF; // 取低32位
        byte[] timestampBytes = new byte[4];
        timestampBytes[0] = (byte) (timestamp >> 24);
        timestampBytes[1] = (byte) (timestamp >> 16);
        timestampBytes[2] = (byte) (timestamp >> 8);
        timestampBytes[3] = (byte) timestamp;
        
        int checksum = 0;

        if (sessionContext.isBluetooth() && sessionContext.getMac() != null) {
            // 蓝牙设备心跳包
            // Header(2)+Length(1)+Type(1)+MAC(6)+Data(5)+Checksum(1) = 16 bytes
            byte[] fullPacket = new byte[16];
            fullPacket[0] = 0x55; // 同步头
            fullPacket[1] = 0x56; // 同步头
            fullPacket[2] = 0x06; // 长度 (MAC 6 + 数据 5)
            fullPacket[3] = HEARTBEAT_TYPE; // 心跳类型
            
            // 添加MAC地址
            byte[] macByte = ParseTcpDataUtil.hexStringToByteArray(sessionContext.getMac());
            System.arraycopy(macByte, 0, fullPacket, 4, 6);
            
            // 添加应用层心跳数据
            fullPacket[10] = heartbeatData; // 心跳标志
            // 添加时间戳
            System.arraycopy(timestampBytes, 0, fullPacket, 11, 4);
            
            // 计算校验和
            checksum = calculateChecksum(fullPacket, 0, 14);
            fullPacket[15] = (byte) (checksum & 0xFF);
            
            out.write(fullPacket);
        } else {
            // 普通设备心跳包
            // Header(2)+Length(1)+Type(1)+Data(5)+Checksum(1) = 10 bytes
            byte[] fullPacket = new byte[10];
            fullPacket[0] = 0x55; // 同步头
            fullPacket[1] = 0x56; // 同步头
            fullPacket[2] = 0x05; // 长度 (数据 5)
            fullPacket[3] = HEARTBEAT_TYPE; // 心跳类型
            
            // 添加应用层心跳数据
            fullPacket[4] = heartbeatData; // 心跳标志
            // 添加时间戳
            System.arraycopy(timestampBytes, 0, fullPacket, 5, 4);
            
            // 计算校验和
            checksum = calculateChecksum(fullPacket, 0, 8);
            fullPacket[9] = (byte) (checksum & 0xFF);
            out.write(fullPacket);
        }
        out.flush();
        // 记录心跳发送时间
        sessionContext.setLastHeartbeatTime(System.currentTimeMillis());
    }
    
    private int calculateChecksum(byte[] data, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += (data[i] & 0xFF);
        }
        return sum;
    }

    private void cancelHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
    }
    
    /**
     * 处理网络异常
     * @param e 异常对象
     */
    private void handleNetworkException(Exception e) {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否是网络恢复
        if (currentTime - lastNetworkExceptionTime > NETWORK_RECOVERY_THRESHOLD) {
            networkExceptionCount = 0;
            log.info("Network recovered, resetting exception count. Client: {}", sessionContext.getClientIP());
        }
        
        // 增加网络异常计数
        networkExceptionCount++;
        lastNetworkExceptionTime = currentTime;
        
        //log.warn("Network exception detected (count: {}), error: {}. Client: {}",
        //        networkExceptionCount, e.getMessage(), sessionContext.getClientIP());
        
        // 如果网络异常次数过多，考虑关闭连接
        if (networkExceptionCount >= MAX_NETWORK_EXCEPTIONS) {
            //log.warn("Too many network exceptions ({}) detected, closing connection. Client: {}",
            //        networkExceptionCount, sessionContext.getClientIP());
            isHeartbeatActive = false;
            // 这里不立即断开，让主循环自然退出
        }
    }
}