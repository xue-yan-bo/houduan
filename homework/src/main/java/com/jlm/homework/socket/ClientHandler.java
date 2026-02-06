package com.jlm.homework.socket;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
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
    
    // Heartbeat
    private ScheduledExecutorService heartbeatScheduler;
    private static final byte HEARTBEAT_TYPE = 0x05;
    private static final long HEARTBEAT_INTERVAL = 6;

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IHandlerService handlerService, SessionContext sessionContext) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.handlerService = handlerService;
        this.sessionContext = sessionContext;
        this.preReadBytes = null;
    }

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IHandlerService handlerService, SessionContext sessionContext, byte[] preReadBytes) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.handlerService = handlerService;
        this.sessionContext = sessionContext;
        this.preReadBytes = preReadBytes;
    }

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
                    
                    Packet packet = decoder.readNextPacket();
                    if (packet == null) {
                        // Socket仍然连接，可能是暂时没有数据，继续循环
                        Thread.sleep(100);
                        continue;
                    }
                    if (packet.isBluetooth()) {
                        handleBluetoothPreProcess(packet);
                    } else {
                        sessionContext.setBluetooth(false);
                    }

                    // Dispatch
                    MessageHandler handler = handlers.get(packet.getType());
                    if (handler != null) {
                        try {
                            handler.handle(sessionContext, packet, responseSender);
                        } catch (Exception e) {
                            log.error("Error handling packet type {}", packet.getType(), e);
                            // responseSender.sendText("Error processing packet: " + e.getMessage());
                        }
                    } else {
                        log.warn("Unknown packet type: {}", String.format("0x%02X", packet.getType()));
                    }

                } catch (InterruptedException e) {
                    // 线程被中断，继续运行
                    //log.debug("ClientHandler thread interrupted, continuing...");
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
                    // 其他异常，继续运行
                    log.debug("ClientHandler exception, continuing...: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            log.info("Client handler exception: {}", e.getMessage());
        } finally {
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
            log.info("Client connection closed: {}", sessionContext.getClientIP());
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
    public synchronized void saveSessionContextToRedis() {
        if (redisTemplate != null && redisKey != null) {
            try {
                // 先获取最新的SessionContext，然后合并修改，最后保存回去
                Object sessionObj = redisTemplate.opsForValue().get(redisKey);
                if (sessionObj instanceof SessionContext) {
                    SessionContext latestSessionContext = (SessionContext) sessionObj;
                    // 合并修改，保留最新的状态
                    latestSessionContext.setRemoteAddress(sessionContext.getRemoteAddress());
                    latestSessionContext.setClientIP(sessionContext.getClientIP());
                    latestSessionContext.setClientPort(sessionContext.getClientPort());
                    latestSessionContext.setRelation(sessionContext.getRelation());
                    latestSessionContext.setBluetooth(sessionContext.isBluetooth());
                    latestSessionContext.setMac(sessionContext.getMac());
                    latestSessionContext.setHomeworkFlag(sessionContext.isHomeworkFlag());
                    latestSessionContext.setEmendFlag(sessionContext.isEmendFlag());
                    latestSessionContext.setFeedbackFlag(sessionContext.isFeedbackFlag());
                    latestSessionContext.setErrorTitleFlag(sessionContext.isErrorTitleFlag());
                    latestSessionContext.setCopybookFlag(sessionContext.isCopybookFlag());
                    latestSessionContext.setMenuFlag(sessionContext.isMenuFlag());
                    latestSessionContext.setCurrentMenu(sessionContext.getCurrentMenu());
                    latestSessionContext.setHomeworkMenu(sessionContext.getHomeworkMenu());
                    latestSessionContext.setEmendMenu(sessionContext.getEmendMenu());
                    latestSessionContext.setFeedbackMenu(sessionContext.getFeedbackMenu());
                    latestSessionContext.setErrorTitleMenu(sessionContext.getErrorTitleMenu());
                    latestSessionContext.setCopybookMenu(sessionContext.getCopybookMenu());
                    latestSessionContext.setConfirmCount(sessionContext.getConfirmCount());
                    latestSessionContext.setWork2Boards(sessionContext.getWork2Boards());
                    latestSessionContext.setEmendBoards(sessionContext.getEmendBoards());
                    latestSessionContext.setCopybookBoards(sessionContext.getCopybookBoards());
                    latestSessionContext.setButtonTimes(sessionContext.getButtonTimes());
                    latestSessionContext.setHomeId(sessionContext.getHomeId());
                    latestSessionContext.setCopybookId(sessionContext.getCopybookId());
                    latestSessionContext.setPageNum(sessionContext.getPageNum());
                    // 合并笔记书写信息
                    latestSessionContext.setStudentClassRecords(sessionContext.getStudentClassRecords());
                    latestSessionContext.setStudentsWriteRecords(sessionContext.getStudentsWriteRecords());
                    latestSessionContext.setStudentsEmendRecords(sessionContext.getStudentsEmendRecords());
                    latestSessionContext.setStudentsFeedbackRecords(sessionContext.getStudentsFeedbackRecords());
                    latestSessionContext.setUploadErrorTitleRecords(sessionContext.getUploadErrorTitleRecords());
                    latestSessionContext.setStudentsCopybookRecords(sessionContext.getStudentsCopybookRecords());
                    latestSessionContext.setLastList(sessionContext.getLastList());
                    // 保存合并后的SessionContext
                    redisTemplate.opsForValue().set(redisKey, latestSessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                    // 更新本地SessionContext
                    sessionContext = latestSessionContext;
                } else {
                    // 不存在，直接保存
                    redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                }
                log.info("Saved session context to Redis for client: {}", sessionContext.getClientIP());
            } catch (Exception e) {
                log.warn("Failed to save session context to Redis: {}", e.getMessage());
            }
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