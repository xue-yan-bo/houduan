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
        this.sessionContext = sessionContext;
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

        // 尝试获取真实地址
        InetSocketAddress realAddress = originalAddress;
        if (proxyDecoder != null) {
            InetSocketAddress proxyAddress = proxyDecoder.getRealAddress();
            if (proxyAddress != null) {
                realAddress = proxyAddress;
                log.info("Using real client address from proxy: {}", realAddress);
            }
        }

        String realIp = realAddress.getHostString();
        
        // 根据真实 IP 构建 Redis key
        redisKey = redisKeyPrefix + realIp;
        log.info("Redis key for client: {}", redisKey);

        // 从 Redis 获取或创建 SessionContext
        Object sessionObj = redisTemplate.opsForValue().get(redisKey);
        if (sessionObj instanceof SessionContext) {
            sessionContext = (SessionContext) sessionObj;
            log.debug("Found existing session context in Redis for client: {}", realIp);
        } else {
            // 创建新的 SessionContext
            sessionContext = new SessionContext();
            log.debug("Created new session context for client: {}", realIp);
        }

        // 更新 SessionContext 中的地址信息
        sessionContext.setRemoteAddress(realAddress);
        sessionContext.setClientIP(realIp);
        sessionContext.setClientPort(realAddress.getPort());

        // 保存 SessionContext 到 Redis
        redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);

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
        ctx.close();
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
