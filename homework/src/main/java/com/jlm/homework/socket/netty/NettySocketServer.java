package com.jlm.homework.socket.netty;

import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.context.SessionContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class NettySocketServer implements SmartLifecycle {

    private static final String REDIS_KEY_PREFIX = "socket:client:";

    @Value("${socket.server.port:6000}")
    private int socketPort;

    @Value("${socket.server.maxConnections:500}")
    private int maxConnections;

    @Value("${socket.server.bossThreads:1}")
    private int bossThreads;

    @Value("${socket.server.workerThreads:0}")
    private int workerThreads;

    @Value("${socket.server.idleTimeout:300}")
    private int idleTimeout;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;

    @Autowired
    private IHandlerService handlerService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;

    private int bossThreadCount;
    private int workerThreadCount;

    private final AtomicInteger connectionCount = new AtomicInteger(0);

    @Override
    public void start() {
        int workers = workerThreads > 0 ? workerThreads : Runtime.getRuntime().availableProcessors() * 2;

        this.bossThreadCount = bossThreads;
        this.workerThreadCount = workers;

        bossGroup = new NioEventLoopGroup(bossThreadCount, new NamedThreadFactory("netty-boss"));
        workerGroup = new NioEventLoopGroup(workerThreadCount, new NamedThreadFactory("netty-worker"));

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 128 * 1024)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            if (connectionCount.incrementAndGet() > maxConnections) {
                                log.warn("Connection limit reached: {}, closing {}", 
                                    maxConnections, ch.remoteAddress());
                                connectionCount.decrementAndGet();
                                ch.close();
                                return;
                            }

                            InetSocketAddress realAddress = (InetSocketAddress) ch.remoteAddress();
                            String clientIp = realAddress.getHostString();

                            SessionContext sessionContext = getOrCreateSessionContext(clientIp);
                            sessionContext.setRemoteAddress(realAddress);
                            sessionContext.setClientIP(clientIp);
                            sessionContext.setClientPort(realAddress.getPort());

                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                idleTimeout, 0, 0, TimeUnit.SECONDS));

                            pipeline.addLast("packetFrameDecoder", new PacketFrameDecoder());

                            pipeline.addLast("socketServerHandler", new SocketServerHandler(
                                messagingTemplate,
                                smartDeviceUserRelationService,
                                handlerService,
                                sessionContext,
                                redisTemplate,
                                REDIS_KEY_PREFIX + clientIp,
                                connectionCount
                            ));
                        }
                    });

            ChannelFuture future = bootstrap.bind(socketPort).sync();
            serverChannel = future.channel();
            running = true;

            log.info("Netty Socket Server started on port {}, workers: {}, maxConnections: {}", 
                socketPort, workers, maxConnections);

            startMonitor();

        } catch (InterruptedException e) {
            log.error("Failed to start Netty server", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("Netty Socket Server stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private SessionContext getOrCreateSessionContext(String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + clientIp;
        Object sessionObj = redisTemplate.opsForValue().get(redisKey);

        if (sessionObj instanceof SessionContext) {
            return (SessionContext) sessionObj;
        }

        SessionContext context = new SessionContext();
        redisTemplate.opsForValue().set(redisKey, context, 24, TimeUnit.HOURS);
        return context;
    }

    private void startMonitor() {
        workerGroup.scheduleAtFixedRate(() -> {
            int connections = connectionCount.get();
            
            log.info("Netty connections: {}, workerThreads: {}, bossThreads: {}",
                connections, workerThreadCount, bossThreadCount);

            if (connections > maxConnections * 0.9) {
                log.warn("Connection count is nearly at limit: {}/{}", connections, maxConnections);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}
