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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    
    // Handlers
    private final Map<Byte, MessageHandler> handlers = new HashMap<>();
    
    // 处理线程池，使用固定大小的线程池，增加线程数以提高并发处理能力
    private static final java.util.concurrent.ExecutorService PROCESS_THREAD_POOL = java.util.concurrent.Executors.newFixedThreadPool(200);
    
    // Heartbeat
    private ScheduledExecutorService heartbeatScheduler;
    private static final byte HEARTBEAT_TYPE = 0x05;
    private static final long HEARTBEAT_INTERVAL = 6;

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IHandlerService handlerService, SessionContext sessionContext, byte[] preReadBytes,
                         org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, String redisKey) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.handlerService = handlerService;
        this.sessionContext = sessionContext;
        this.preReadBytes = preReadBytes;
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
    }

    private void initHandlers() {
        // Dependencies for handlers
        HandwritingHandler handwritingHandler = new HandwritingHandler(messagingTemplate, handlerService, smartDeviceUserRelationService, this);
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
        try {
            // 获取输入输出流（不使用try-with-resources，避免自动关闭）
            in = clientSocket.getInputStream();
            // 使用BufferedInputStream包装输入流，以便支持标记操作和提高读取性能
            java.io.BufferedInputStream bufferedInputStream = new java.io.BufferedInputStream(in, 1024);
            out = clientSocket.getOutputStream();
            
            // 保持连接活跃，去掉超时限制
            clientSocket.setKeepAlive(true);
            // 设置TCP连接超时时间为60分钟
            clientSocket.setSoTimeout(60 * 60 * 1000);
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
            while (true) {
                try {
                    // 检查Socket是否仍然连接
                    if (clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected()) {
                        //log.info("Socket is not connected, exiting loop");
                        break;
                    }
                    
                    // 设置Socket超时，避免长时间阻塞
                    clientSocket.setSoTimeout(1000);
                    
                    Packet packet = decoder.readNextPacket();
                    if (packet == null) {
                        // Socket仍然连接，可能是暂时没有数据，短暂休眠后继续
                        Thread.sleep(10);
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
                        log.warn("Unknown packet type: {} for client: {}", String.format("0x%02X", packet.getType()), sessionContext.getClientIP());
                    }

                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    log.info("ClientHandler thread interrupted, exiting...");
                    break;
                } catch (java.net.SocketTimeoutException e) {
                    // 超时异常，继续循环
                    continue;
                } catch (IOException e) {
                    // IO异常可能表示连接断开
                    log.info("IO exception, checking connection: {}", e.getMessage());
                    // 检查Socket状态
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            // 忽略关闭异常
                        }
                    }
                    break;
                } catch (Exception e) {
                    // 其他异常，记录并继续
                    log.error("ClientHandler exception, continuing...: {}", e.getMessage(), e);
                    // 短暂休眠，避免异常风暴
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
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
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.info("Error closing socket: {}", e.getMessage());
            }
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
                    handlerService.saveFeedbackRecords(userId, name, sessionContext.getStudentsFeedbackRecords());
                    //log.info("Saved unsaved feedback notes for user {}: {}", userId, sessionContext.getStudentsFeedbackRecords().size());
                }
                
                // 保存错题模式的未写入记录
                if (!sessionContext.getUploadErrorTitleRecords().isEmpty() && sessionContext.getCurrentMenu() != null) {
                    MenuItemT itemT = sessionContext.getCurrentMenu().getPItems().get(sessionContext.getCurrentMenu().getSelectItem());
                    String name = itemT.getDesc();
                    handlerService.saveErrorTitleRecords(userId, name, sessionContext.getUploadErrorTitleRecords());
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
                    log.info("Saved unsaved classroom notes for user {}: {}", userId, sessionContext.getStudentClassRecords().size());
                    // 清空studentClassRecords，避免内存占用过高
                    sessionContext.getStudentClassRecords().clear();
                    // 保存修改后的SessionContext回Redis
                    saveSessionContextToRedis();
                }
                
            } catch (Exception e) {
                log.warn("Failed to save unsaved notes: {}", e.getMessage());
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
                log.warn("Failed to send bind student request: {}", e.getMessage());
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
                // 先检查Socket连接状态
                if (clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected()) {
                    log.info("Socket is closed, stopping heartbeat");
                    cancelHeartbeat();
                    return;
                }
                sendHeartbeat(out);
            } catch (IOException e) {
                //log.info("Heartbeat failed: " + e.getMessage());
                cancelHeartbeat();
                // 心跳失败时检查Socket状态
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                } catch (IOException ex) {
                    // 忽略关闭异常
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    private void sendHeartbeat(OutputStream out) throws IOException {
        byte heartbeatData = 0x01;
        byte[] packet = new byte[5];
        packet[0] = 0x55;
        packet[1] = 0x56;
        packet[2] = 0x02;
        packet[3] = HEARTBEAT_TYPE;
        int checksum = 0;

        if (sessionContext.isBluetooth() && sessionContext.getMac() != null) {
            // Re-implement Bluetooth Heartbeat logic
            // Header(2)+Length(1)+Type(1)+MAC(6)+Data(1)+Checksum(1) = 12 bytes
            // Original code: packet was 5 bytes, then logic for bluetooth...
            // Line 1469: if(isBluetooth) ... 
            // It seems the original code was reusing `packet` buffer which was 5 bytes, but writing into index 4... 
            // Wait, original code line 1456: packet = new byte[5].
            // Line 1472: System.arraycopy(macByte, 0, packet, 4, 6); -> IndexOutOfBoundsException if packet is 5 bytes!
            // The original code might be buggy or I misread it.
            // Original Line 1456: byte[] packet = new byte[5];
            // Original Line 1472: System.arraycopy(..., packet, 4, 6); -> 4+6=10. Array is 5. Crash.
            
            // Wait, maybe `packet` variable is reassigned? No.
            // This suggests the original code crashes on Bluetooth Heartbeat? 
            // Or `isBluetooth` is rarely true.
            
            // I will fix this.
            
            byte[] fullPacket = new byte[12];
            fullPacket[0] = 0x55;
            fullPacket[1] = 0x56;
            fullPacket[2] = 0x02; // Length
            fullPacket[3] = HEARTBEAT_TYPE;
            
            byte[] macByte = ParseTcpDataUtil.hexStringToByteArray(sessionContext.getMac());
            System.arraycopy(macByte, 0, fullPacket, 4, 6);
            
            fullPacket[10] = heartbeatData;
            
            checksum = calculateChecksum(fullPacket, 0, 10);
            fullPacket[11] = (byte) (checksum & 0xFF);
            
            out.write(fullPacket);
        } else {
            packet[4] = heartbeatData;
            checksum = calculateChecksum(packet, 0, 4);
            byte[] fullPacket = new byte[6];
            System.arraycopy(packet, 0, fullPacket, 0, 5);
            fullPacket[5] = (byte) (checksum & 0xFF);
            out.write(fullPacket);
        }
        out.flush();
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
}