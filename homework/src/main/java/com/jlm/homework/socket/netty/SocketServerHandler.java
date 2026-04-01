package com.jlm.homework.socket.netty;

import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.HandwritingParseResult;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.handler.*;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.util.ParseTcpDataUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ChannelHandler.Sharable
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final IHandlerService handlerService;
    private SessionContext sessionContext;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String redisKeyPrefix;
    private String redisKey;
    private final AtomicInteger connectionCount;
    private final ProxyProtocolDecoder proxyDecoder;

    private NettyResponseSender responseSender;
    private final Map<Byte, MessageHandler> handlers = new HashMap<>();
    private boolean realAddressResolved = false;

    private ScheduledFuture<?> heartbeatFuture;
    private static final byte HEARTBEAT_TYPE = 0x05;
    private static final long HEARTBEAT_INTERVAL = 8;

    public SocketServerHandler(
            SimpMessagingTemplate messagingTemplate,
            ISmartDeviceUserRelationService smartDeviceUserRelationService,
            IHandlerService handlerService,
            SessionContext sessionContext,
            RedisTemplate<String, Object> redisTemplate,
            String redisKeyPrefix,
            AtomicInteger connectionCount,
            ProxyProtocolDecoder proxyDecoder) {
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.handlerService = handlerService;
        this.sessionContext = null;
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefix = redisKeyPrefix;
        this.connectionCount = connectionCount;
        this.proxyDecoder = proxyDecoder;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 获取原始地址
        InetSocketAddress originalAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        log.info("Channel active: {}", originalAddress);

        // 初始化 SessionContext（临时使用原始地址）
        sessionContext = new SessionContext();
        sessionContext.setRemoteAddress(originalAddress);
        sessionContext.setClientIP(originalAddress.getHostString());
        sessionContext.setClientPort(originalAddress.getPort());

        // 临时构建 Redis key
        redisKey = redisKeyPrefix + originalAddress.getHostString();

        responseSender = new NettyResponseSender(ctx, sessionContext);

        initHandlers();

        ScheduledFuture<?> future = ctx.executor().scheduleAtFixedRate(
            () -> sendHeartbeat(ctx),
            HEARTBEAT_INTERVAL,
            HEARTBEAT_INTERVAL,
            TimeUnit.SECONDS
        );
        heartbeatFuture = future;

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel inactive: {}", ctx.channel().remoteAddress());

        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }

        saveUnsavedData();
        saveSessionContextToRedis();

        connectionCount.decrementAndGet();

        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }

        // 第一次收到数据时，尝试解析真实 IP
        if (!realAddressResolved && proxyDecoder != null) {
            InetSocketAddress originalAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            InetSocketAddress realAddress = originalAddress;
            
            InetSocketAddress proxyAddress = proxyDecoder.getRealAddress();
            if (proxyAddress != null) {
                realAddress = proxyAddress;
                log.info("Using real client address from proxy: {}", realAddress);
            }
            
            // 无论是否有代理，都使用真实 IP 构建 Redis key
            String realIp = realAddress.getHostString();
            redisKey = redisKeyPrefix + realIp;
            log.info("Redis key for client: {}", redisKey);
            
            // 从 Redis 获取或创建 SessionContext
            Object sessionObj = redisTemplate.opsForValue().get(redisKey);
            if (sessionObj instanceof SessionContext) {
                sessionContext = (SessionContext) sessionObj;
                log.debug("Found existing session context in Redis for client: {}", realIp);
            } else {
                sessionContext = new SessionContext();
                log.debug("Created new session context for client: {}", realIp);
            }
            
            // 更新 SessionContext 中的地址信息
            sessionContext.setRemoteAddress(realAddress);
            sessionContext.setClientIP(realIp);
            sessionContext.setClientPort(realAddress.getPort());
            
            // 保存 SessionContext 到 Redis
            redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
            
            // 更新 responseSender 使用新的 sessionContext
            if (responseSender != null) {
                responseSender = new NettyResponseSender(ctx, sessionContext);
            }
            
            realAddressResolved = true;
        }

        ByteBuf packet = (ByteBuf) msg;
        try {
            processPacket(ctx, packet);
        } finally {
            packet.release();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.warn("Channel idle, closing: {}", ctx.channel().remoteAddress());
            ctx.close();
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Channel exception: {}", cause.getMessage(), cause);
        
        // 检查异常类型，避免因为非致命异常而关闭通道
        if (cause instanceof java.io.IOException) {
            String errorMsg = cause.getMessage();
            if (errorMsg != null && (errorMsg.contains("断开的管道") || errorMsg.contains("Broken pipe") || 
                errorMsg.contains("Connection reset") || errorMsg.contains("Socket closed") ||
                errorMsg.contains("你的主机中的软件中止了一个已建立的连接") ||
                errorMsg.contains("An existing connection was forcibly closed by the remote host"))) {
                // 这些是连接断开的致命错误，需要关闭通道
                log.warn("Fatal connection error, closing channel: {}", errorMsg);
                ctx.close();
            } else {
                // 其他IO异常，可能是临时错误，记录但不关闭通道
                log.warn("Non-fatal IO error, continuing: {}", errorMsg);
            }
        } else if (cause instanceof java.net.SocketException) {
            String errorMsg = cause.getMessage();
            if (errorMsg != null && (errorMsg.contains("Connection reset") || errorMsg.contains("Socket closed") ||
                errorMsg.contains("你的主机中的软件中止了一个已建立的连接") ||
                errorMsg.contains("An existing connection was forcibly closed by the remote host"))) {
                // 这些是连接断开的致命错误，需要关闭通道
                log.warn("Fatal socket error, closing channel: {}", errorMsg);
                ctx.close();
            } else {
                // 其他Socket异常，可能是临时错误，记录但不关闭通道
                log.warn("Non-fatal socket error, continuing: {}", errorMsg);
            }
        } else if (cause instanceof java.lang.OutOfMemoryError) {
            // 内存溢出错误，需要关闭通道
            log.error("Out of memory error, closing channel: {}", cause.getMessage());
            ctx.close();
        } else {
            // 其他异常，可能是业务逻辑错误，记录但不关闭通道
            log.warn("Non-fatal exception, continuing: {}", cause.getMessage());
        }
    }

    private void initHandlers() {
        HandwritingHandler handwritingHandler = new HandwritingHandler(
            messagingTemplate, handlerService, smartDeviceUserRelationService, this);
        ButtonHandler buttonHandler = new ButtonHandler(
            messagingTemplate, handlerService, responseSender, smartDeviceUserRelationService, this);
        SerialNumberHandler serialNumberHandler = new SerialNumberHandler(
            smartDeviceUserRelationService, messagingTemplate, this);

        handlers.put((byte) 0x01, handwritingHandler);
        handlers.put((byte) 0x81, handwritingHandler);
        handlers.put((byte) 0x02, buttonHandler);
        handlers.put((byte) 0x03, serialNumberHandler);
    }

    private void processPacket(ChannelHandlerContext ctx, ByteBuf packet) {
        if (packet.readableBytes() < 4) {
            log.warn("Packet too short");
            return;
        }

        // 保存完整的数据包
        byte[] fullPacket = new byte[packet.readableBytes()];
        packet.getBytes(packet.readerIndex(), fullPacket);

        // 解析数据包
        packet.skipBytes(2);
        int length = packet.readUnsignedByte();
        byte type = packet.readByte();

        byte[] data = new byte[length];
        packet.readBytes(data);

        Packet packetObj = new Packet(type, data);
        packetObj.setRawData(fullPacket);

        MessageHandler handler = handlers.get(type);
        if (handler != null) {
            try {
                handler.handle(sessionContext, packetObj, responseSender);
            } catch (Exception e) {
                log.error("Error handling packet type {}: {}", type, e.getMessage(), e);
            }
        } else {
            log.warn("Unknown packet type: {}", type);
        }
    }

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        try {
            byte[] heartbeatData = new byte[10];
            heartbeatData[0] = 0x55;
            heartbeatData[1] = 0x56;
            heartbeatData[2] = 0x05;
            heartbeatData[3] = HEARTBEAT_TYPE;
            heartbeatData[4] = 0x01;

            long timestamp = System.currentTimeMillis() & 0xFFFFFFFF;
            heartbeatData[5] = (byte) (timestamp >> 24);
            heartbeatData[6] = (byte) (timestamp >> 16);
            heartbeatData[7] = (byte) (timestamp >> 8);
            heartbeatData[8] = (byte) timestamp;

            int checksum = 0;
            for (int i = 0; i < 9; i++) {
                checksum += (heartbeatData[i] & 0xFF);
            }
            heartbeatData[9] = (byte) (checksum & 0xFF);

            ctx.writeAndFlush(io.netty.buffer.Unpooled.wrappedBuffer(heartbeatData));
        } catch (Exception e) {
            log.error("Error sending heartbeat: {}", e.getMessage(), e);
            // 心跳发送失败，不关闭通道，继续尝试
        }
    }

    private void saveUnsavedData() {
        try {
            if (sessionContext.getRelation() != null &&
                sessionContext.getRelation().getUserId() != null) {

                Long userId = Long.parseLong(sessionContext.getRelation().getUserId());

                if (!sessionContext.getStudentsWriteRecords().isEmpty() &&
                    sessionContext.getHomeId() != null &&
                    sessionContext.getPageNum() != null) {
                    handlerService.saveWriteRecords(
                        userId, sessionContext.getHomeId(), "1",
                        sessionContext.getPageNum(),
                        sessionContext.getStudentsWriteRecords(), false);
                }

                if (!sessionContext.getStudentsCopybookRecords().isEmpty() &&
                    sessionContext.getCopybookId() != null &&
                    sessionContext.getPageNum() != null) {
                    handlerService.saveWriteRecords(
                        userId, sessionContext.getCopybookId(), "2",
                        sessionContext.getPageNum(),
                        sessionContext.getStudentsCopybookRecords(), false);
                }
                
                // 保存未保存的反馈数据
                if (!sessionContext.getStudentsFeedbackRecords().isEmpty()) {
                    handlerService.saveFeedbackRecords(
                        sessionContext.getFeedbackId(), userId, 
                        sessionContext.getFeedbackSubject(), 
                        sessionContext.getStudentsFeedbackRecords());
                }
            }
        } catch (Exception e) {
            log.error("Error saving unsaved data: {}", e.getMessage(), e);
        }
    }

    public void saveSessionContextToRedis() {
        try {
            redisTemplate.opsForValue().set(redisKey, sessionContext, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error saving session context to Redis: {}", e.getMessage(), e);
        }
    }

    public void saveHardDataToRedis(String hex) {
        try {
//            redisTemplate.opsForValue().set("hard:" + sessionContext.getMac(), hex, 1, TimeUnit.HOURS);
            String key = "hard:list:" + sessionContext.getMac();

// 1. 将 hex 添加到 list 的右侧（也可以用 leftPush）
            redisTemplate.opsForList().rightPush(key, hex);

// 2. 设置整个 key（即这个 list）1 小时后过期
            redisTemplate.expire(key, 1, TimeUnit.HOURS);


        } catch (Exception e) {
            log.error("Error saving hard data to Redis: {}", e.getMessage(), e);
        }
    }
    /**
     * 获取RedisTemplate
     * @return RedisTemplate实例
     */
    public org.springframework.data.redis.core.RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
