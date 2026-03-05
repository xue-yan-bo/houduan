package com.jlm.homework.socket;

import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.socket.context.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class SocketService implements SmartLifecycle {
    
    // Redis key prefix for client handler mapping
    private static final String REDIS_KEY_PREFIX = "socket:client:";
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;
    @Autowired
    private IHandlerService handlerService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ThreadPoolExecutor threadPool;
    private ServerSocket serverSocket;
    private boolean running = false;

    @Value("${socket.server.port:6000}")
    private int socketPort; // 可以通过配置文件管理端口
    @Value("${socket.server.maxConnections:10}")
    private int maxConnections;
    @Value("${socket.server.corePoolSize:50}")
    private int corePoolSize; // 核心线程数
    @Value("${socket.server.maxPoolSize:500}")
    private int maxPoolSize; // 最大线程数
    @Value("${socket.server.queueCapacity:2000}")
    private int queueCapacity; // 队列容量

    // 连接计数器
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    // 拒绝任务计数器
    private final AtomicLong rejectedTaskCount = new AtomicLong(0);
    // 最大连接数
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;
    
    // 连接状态监控
    private ScheduledExecutorService monitorScheduler;
    private static final long MONITOR_INTERVAL = 30; // 监控间隔，30秒

    @Override
    public void start() {
        // 使用ThreadPoolExecutor，配置合理的参数
        // 核心线程数：50，最大线程数：500，队列容量：2000
        // 使用CallerRunsPolicy拒绝策略，当队列满时由调用者线程执行任务
        threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "socket-handler-" + threadNumber.getAndIncrement());
                        t.setDaemon(false);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用者线程执行
        );
        
        // 允许核心线程超时
        threadPool.allowCoreThreadTimeOut(true);
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(socketPort);
                serverSocket.setReuseAddress(true);
                serverSocket.setReceiveBufferSize(128 * 1024); // 增加接收缓冲区到128KB
                serverSocket.setPerformancePreferences(1, 10, 1); // 优先考虑延迟，提高响应速度
                running = true;
                log.info("Socket server started on port {}, corePoolSize: {}, maxPoolSize: {}, queueCapacity: {}", 
                        socketPort, corePoolSize, maxPoolSize, queueCapacity);
                
                // 启动连接状态监控
                startConnectionMonitor();

                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        // 检查连接数是否超过限制
                        int currentConnections = connectionCount.incrementAndGet();
                        if (currentConnections > maxPoolSize) {
                            log.warn("Connection limit reached: {}, closing new connection from {}",
                                    maxPoolSize, socket.getRemoteSocketAddress());
                            connectionCount.decrementAndGet();
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // 忽略关闭异常
                            }
                            continue;
                        }
                        
                       // log.info("New client connected, raw address: {}", socket.getRemoteSocketAddress());

                        // 解析代理头，获取真实客户端地址
                        ProxyHeaderResult proxyResult;
                        try {
                            proxyResult = parseProxyHeader(socket);
                        } catch (IOException e) {
                            //log.warn("Failed to parse proxy header, using default address: {}", e.getMessage());
                            proxyResult = new ProxyHeaderResult(
                                (InetSocketAddress) socket.getRemoteSocketAddress(),
                                null
                            );
                        }
                        
                        // 使用真实客户端地址
                        InetSocketAddress realAddress = proxyResult.getRealAddress();
                        String clientAddress = realAddress.getHostString();
                        int clientPort = realAddress.getPort();
                        
                        //log.info("Client connected with real address: {}", clientAddress);
                        
                        // 使用Redis获取或创建SessionContext
                        SessionContext sessionContext = null;
                        String redisKey = REDIS_KEY_PREFIX + clientAddress;
                        
                        // 从Redis获取SessionContext
                        Object sessionObj = redisTemplate.opsForValue().get(redisKey);
                        if (sessionObj instanceof SessionContext) {
                            sessionContext = (SessionContext) sessionObj;
                            //log.debug("Found existing session context in Redis for client: {}", clientAddress);
                        } else {
                            // 创建新的SessionContext
                            sessionContext = new SessionContext();
                            // 保存到Redis，设置过期时间为24小时
                            redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                            //log.debug("Created new session context and saved to Redis for client: {}", clientAddress);
                        }
                        
                        // 设置真实客户端地址到SessionContext
                        sessionContext.setRemoteAddress(realAddress);
                        sessionContext.setClientIP(clientAddress);
                        sessionContext.setClientPort(clientPort);
                        // 保存修改后的SessionContext回Redis
                        redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                        //log.debug("Saved session context to Redis for client: {}", clientAddress);
                        
                        // 提交客户端连接到线程池处理
                        ClientHandler clientHandler = new ClientHandler(socket, messagingTemplate,
                                smartDeviceUserRelationService, handlerService, sessionContext, proxyResult.getPreReadBytes(),
                                redisTemplate, redisKey, proxyResult.getBufferedInputStream());

                        threadPool.submit(() -> {
                            try {
                                clientHandler.run();
                            } finally {
                                // 连接处理完成后，减少连接计数
                                connectionCount.decrementAndGet();
                                //log.info("Client connection closed, current connections: {}", connectionCount.get());
                            }
                        });
                        //log.info("Submitted client handler for client: {}, current connections: {}", clientAddress, connectionCount.get());
                    } catch (IOException e) {
                        if (running) {
                            log.error("Error accepting client connection: {}", e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Socket server error: {}", e.getMessage(), e);
                }
            } finally {
                stop();
            }
        }).start();
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // 停止连接状态监控
            stopConnectionMonitor();
            // 优雅关闭线程池
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown(); // 不再接受新任务
                try {
                    // 等待已提交的任务完成
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow(); // 强制关闭
                        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                            log.warn("Thread pool did not terminate");
                        }
                    }
                } catch (InterruptedException ie) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            log.error("Error closing server socket: {}", e.getMessage());
        }
    }
    
    /**
     * 启动连接状态监控
     */
    private void startConnectionMonitor() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor();
        monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                int currentConnections = connectionCount.get();
                long rejectedTasks = rejectedTaskCount.get();
                
                // 获取线程池状态
                int activeThreads = threadPool.getActiveCount();
                int poolSize = threadPool.getPoolSize();
                int corePoolSize = threadPool.getCorePoolSize();
                int maximumPoolSize = threadPool.getMaximumPoolSize();
                long completedTasks = threadPool.getCompletedTaskCount();
                long totalTasks = threadPool.getTaskCount();
                int queueSize = threadPool.getQueue().size();
                int queueRemainingCapacity = threadPool.getQueue().remainingCapacity();
                
                // 记录详细的状态信息
                log.info("Socket server status: connections={}, activeThreads={}, poolSize={}/{}, " +
                        "queueSize={}/{}, completedTasks={}, rejectedTasks={}",
                        currentConnections, activeThreads, poolSize, maximumPoolSize,
                        queueSize, queueCapacity, completedTasks, rejectedTasks);
                
                // 检查线程池健康状态
                if (queueSize > queueCapacity * 0.8) {
                    log.warn("Thread pool queue is nearly full: {}/{}", queueSize, queueCapacity);
                }
                
                if (activeThreads >= maximumPoolSize * 0.9) {
                    log.warn("Thread pool is nearly exhausted: activeThreads={}, maxPoolSize={}", 
                            activeThreads, maximumPoolSize);
                }
                
                // 检查服务器Socket状态
                if (serverSocket != null && !serverSocket.isClosed()) {
                    if (serverSocket.isBound()) {
                        //log.debug("Server socket is bound and listening on port {}", socketPort);
                    } else {
                        log.warn("Server socket is not bound");
                    }
                }
            } catch (Exception e) {
                log.warn("Error in connection monitor: {}", e.getMessage());
            }
        }, MONITOR_INTERVAL, MONITOR_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * 停止连接状态监控
     */
    private void stopConnectionMonitor() {
        if (monitorScheduler != null && !monitorScheduler.isShutdown()) {
            monitorScheduler.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0; // 表示这个组件应该在应用启动时尽早启动
    }

    @Override
    public boolean isAutoStartup() {
        return true; // 自动启动
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
    
    /**
     * 解析代理头，获取真实客户端地址
     * @param socket 客户端Socket连接
     * @return 代理头解析结果，包含真实地址和预读取的字节
     * @throws IOException 解析过程中发生IO异常
     */
    private ProxyHeaderResult parseProxyHeader(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        // 使用BufferedInputStream包装输入流，以便支持标记操作和提高读取性能
        java.io.BufferedInputStream bufferedInputStream = new java.io.BufferedInputStream(inputStream, 1024);
        // 设置标记，以便重置
        bufferedInputStream.mark(1024);
        // 读取前16个字节用于检测代理头（PROXY v2需要至少16字节）
        byte[] signature = new byte[16];
        int bytesRead = 0;

        try {
            bytesRead = bufferedInputStream.read(signature);
            
            // 默认使用Socket远程地址
            InetSocketAddress realAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
            
            // 记录原始地址用于调试
            String originalAddress = realAddress.getHostString();
            //log.debug("Parsing proxy header, original socket address: {}", originalAddress);

            // 更健壮的代理头处理
            if (bytesRead == -1) {
                // 流已关闭，返回默认地址
                log.debug("Stream closed, using original address: {}", originalAddress);
                return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
            } else if (bytesRead < 5) {
                // 读取字节不足，重置输入流并返回默认地址
                try {
                    bufferedInputStream.reset();
                    log.debug("Insufficient bytes read ({}/5), using original address: {}", bytesRead, originalAddress);
                    return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
                } catch (Exception ex) {
                    // reset失败，通过preReadBytes传递已读取的数据
                    byte[] pre = new byte[bytesRead];
                    System.arraycopy(signature, 0, pre, 0, bytesRead);
                    log.debug("Insufficient bytes read ({}/5), reset failed, using preReadBytes", bytesRead);
                    return new ProxyHeaderResult(realAddress, pre, bufferedInputStream);
                }
            } else {
                    // 检测代理协议类型
                try {
                    // 先检查PROXY v2协议（更常见）
                    if (isProxyV2Signature(signature)) {
                        // PROXY v2 协议
                        log.debug("Detected PROXY v2 protocol, parsing...");
                        // 传递已读取的signature数据，避免重复读取
                        return parseProxyV2(socket, bufferedInputStream, signature, bytesRead);
                    }
                    
                    // 检查PROXY v1协议
                    String sigStr = new String(signature, 0, Math.min(5, bytesRead));
                    if (sigStr.startsWith("PROXY")) {
                        // PROXY v1 协议
                        log.debug("Detected PROXY v1 protocol, parsing...");
                        return parseProxyV1(socket, bufferedInputStream);
                    } else {
                        // 不是代理协议，重置输入流
                        log.debug("No proxy protocol detected, using original address: {}", originalAddress);
                        try {
                            bufferedInputStream.reset();
                            // reset成功，数据已回退到流中，不需要preReadBytes
                            return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
                        } catch (Exception ex) {
                            // reset失败，通过preReadBytes传递已读取的数据
                            byte[] preReadBytes = new byte[bytesRead];
                            System.arraycopy(signature, 0, preReadBytes, 0, bytesRead);
                            return new ProxyHeaderResult(realAddress, preReadBytes, bufferedInputStream);
                        }
                    }
                } catch (Exception ex) {
                    // 解析签名时发生异常，重置输入流并返回默认地址
                    log.warn("Exception parsing proxy header: {}, using original address: {}", ex.getMessage(), originalAddress);
                    try {
                        bufferedInputStream.reset();
                        return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
                    } catch (Exception resetEx) {
                        // reset失败，通过preReadBytes传递已读取的数据
                        byte[] preReadBytes = new byte[bytesRead];
                        System.arraycopy(signature, 0, preReadBytes, 0, bytesRead);
                        return new ProxyHeaderResult(realAddress, preReadBytes, bufferedInputStream);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read proxy header signature: {}, using original address: {}", e.getMessage(), 
                    socket.getRemoteSocketAddress());
            // 重置输入流
            try {
                bufferedInputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }
            return new ProxyHeaderResult(
                    (InetSocketAddress) socket.getRemoteSocketAddress(),
                    null,
                    bufferedInputStream
            );
        }
    }
    
    /**
     * 检测是否为PROXY v2协议签名
     * PROXY v2协议签名：0x0D 0x0A 0x0D 0x0A 0x00 0x0D 0x0A 0x51 0x55 0x49 0x54 0x0A
     * 简化检测：前5字节为 0x0D 0x0A 0x0D 0x0A 0x00
     * @param signature 签名字节
     * @return 是否为PROXY v2协议
     */
    private boolean isProxyV2Signature(byte[] signature) {
        if (signature == null || signature.length < 12) {
            return false;
        }
        try {
            // 检查前5字节的简化签名
            boolean basicMatch = signature[0] == 0x0D && signature[1] == 0x0A && 
                                signature[2] == 0x0D && signature[3] == 0x0A && 
                                signature[4] == 0x00;
            if (!basicMatch) {
                return false;
            }
            // 可选：检查完整12字节签名以提高准确性
            if (signature.length >= 12) {
                return signature[5] == 0x0D && signature[6] == 0x0A &&
                       signature[7] == 0x51 && signature[8] == 0x55 &&
                       signature[9] == 0x49 && signature[10] == 0x54 &&
                       signature[11] == 0x0A;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 解析PROXY v1协议
     * @param socket 客户端Socket
     * @param inputStream 输入流
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV1(Socket socket, InputStream inputStream) throws IOException {
        java.io.BufferedInputStream bufferedInputStream = null;
        try {
            // 尝试将输入流转换为BufferedInputStream，以便支持标记操作
            if (inputStream instanceof java.io.BufferedInputStream) {
                bufferedInputStream = (java.io.BufferedInputStream) inputStream;
            } else {
                bufferedInputStream = new java.io.BufferedInputStream(inputStream, 1024);
                bufferedInputStream.mark(1024);
            }
            // 重置输入流到标记位置
            try {
                bufferedInputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }
            // 重新读取并解析PROXY v1头
            StringBuilder lineBuilder = new StringBuilder();
            int c;
            while ((c = bufferedInputStream.read()) != -1) {
                if (c == '\n') {
                    break;
                }
                if (c != '\r') {
                    lineBuilder.append((char) c);
                }
            }
            String line = lineBuilder.toString();

            if (line.isEmpty() || !line.startsWith("PROXY")) {
                // 无效的PROXY v1头，使用默认地址
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
            }

            String[] parts = line.split(" ");
            if (parts.length < 6) {
                // 无效的PROXY v1格式，使用默认地址
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
            }
            // 获取真实客户端地址
            String clientIp = parts[2];
            int clientPort = Integer.parseInt(parts[4]);
            InetSocketAddress realAddress = new InetSocketAddress(clientIp, clientPort);

            // PROXY v1头已经被读取消费掉了，不需要再reset
            // 后续的PacketDecoder直接从bufferedInputStream继续读取即可

            return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v1 header: {}", e.getMessage());
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
        }
    }
    
    /**
     * 解析PROXY v2协议
     * PROXY v2协议格式：
     * - 0-11字节：固定头部（12字节signature）
     * - 12字节：version/command（高4位version=2，低4位command）
     * - 13字节：protocol/family（高4位protocol，低4位address family）
     * - 14-15字节：address length（2字节，大端序）
     * - 16字节开始：源地址和端口、目标地址和端口
     * @param socket 客户端Socket
     * @param inputStream 输入流（已读取signature）
     * @param signature 已读取的签名字节数组
     * @param bytesRead 已读取的字节数
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV2(Socket socket, java.io.BufferedInputStream inputStream, 
                                          byte[] signature, int bytesRead) throws IOException {
        try {
            // 重置输入流到标记位置（开始位置）
            try {
                inputStream.reset();
            } catch (Exception ex) {
                log.warn("Failed to reset stream in parseProxyV2: {}", ex.getMessage());
                // 如果重置失败，使用已读取的数据
                if (bytesRead >= 16) {
                    return parseProxyV2FromBytes(socket, signature, inputStream);
                }
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, inputStream);
            }
            
            // 使用DataInputStream包装BufferedInputStream
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            // 跳过PROXY v2固定头部的前12个字节（signature）
            dataInputStream.skipBytes(12);

            // 读取version/command字节
            int versionCommand = dataInputStream.readUnsignedByte();
            int version = (versionCommand >> 4) & 0x0F;
            int command = versionCommand & 0x0F;
            
            // 检查version是否为2
            if (version != 2) {
                log.warn("Invalid PROXY v2 version: {}, expected 2", version);
                inputStream.reset();
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, inputStream);
            }
            
            // 检查command：0x01=PROXY, 0x00=LOCAL
            if (command != 0x01) {
                log.debug("PROXY v2 command is LOCAL (0x00), using original address");
                inputStream.reset();
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, inputStream);
            }

            // 读取protocol/family字节
            int protocolFamily = dataInputStream.readUnsignedByte();
            int protocol = (protocolFamily >> 4) & 0x0F;
            int family = protocolFamily & 0x0F;
            
            // 读取address length（2字节，大端序）
            int addressLength = dataInputStream.readUnsignedShort();
            
            // 根据address family解析地址
            String clientIp = null;
            int clientPort = 0;
            
            if (family == 0x01) {
                // IPv4: 源地址4字节 + 源端口2字节 + 目标地址4字节 + 目标端口2字节 = 12字节
                byte[] srcAddressBytes = new byte[4];
                dataInputStream.readFully(srcAddressBytes);
                int srcPort = dataInputStream.readUnsignedShort();
                
                // 跳过目标地址和端口
                dataInputStream.skipBytes(4 + 2);
                
                clientIp = String.format("%d.%d.%d.%d",
                        srcAddressBytes[0] & 0xff,
                        srcAddressBytes[1] & 0xff,
                        srcAddressBytes[2] & 0xff,
                        srcAddressBytes[3] & 0xff);
                clientPort = srcPort;
            } else if (family == 0x02) {
                // IPv6: 源地址16字节 + 源端口2字节 + 目标地址16字节 + 目标端口2字节 = 36字节
                byte[] srcAddressBytes = new byte[16];
                dataInputStream.readFully(srcAddressBytes);
                int srcPort = dataInputStream.readUnsignedShort();
                
                // 跳过目标地址和端口
                dataInputStream.skipBytes(16 + 2);
                
                // 构建IPv6地址字符串
                StringBuilder ipBuilder = new StringBuilder();
                for (int i = 0; i < 16; i += 2) {
                    if (i > 0) ipBuilder.append(":");
                    int high = (srcAddressBytes[i] & 0xff) << 8;
                    int low = srcAddressBytes[i + 1] & 0xff;
                    ipBuilder.append(String.format("%04x", high | low));
                }
                clientIp = ipBuilder.toString();
                clientPort = srcPort;
            } else {
                // 不支持的地址族
                log.warn("Unsupported PROXY v2 address family: {}, using original address", family);
                inputStream.reset();
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, inputStream);
            }

            // 构建真实地址
            InetSocketAddress realAddress = new InetSocketAddress(clientIp, clientPort);
            log.info("Parsed PROXY v2 header successfully, real client address: {}:{}, original: {}", 
                    clientIp, clientPort, socket.getRemoteSocketAddress());
            
            // 计算PROXY v2头部总长度
            // 12字节(signature) + 4字节(version/command/protocol/length) + addressLength字节(地址数据)
            int totalHeaderLength = 12 + 4 + addressLength;
            
            // 重置输入流到开始位置，然后跳过整个PROXY v2头部
            // 这样后续的PacketDecoder就能从实际应用数据开始读取
            try {
                inputStream.reset();
                // 跳过整个PROXY v2头部
                long skipped = dataInputStream.skip(totalHeaderLength);
                if (skipped != totalHeaderLength) {
                    log.warn("Failed to skip complete PROXY v2 header, skipped {}/{} bytes", skipped, totalHeaderLength);
                    // 如果skip失败，尝试读取剩余字节
                    int remaining = (int)(totalHeaderLength - skipped);
                    byte[] buffer = new byte[remaining];
                    int read = dataInputStream.read(buffer);
                    if (read != remaining) {
                        log.warn("Failed to read remaining PROXY v2 header bytes, read {}/{} bytes", read, remaining);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to skip PROXY v2 header after parsing: {}", ex.getMessage());
                // 如果处理失败，尝试重置流
                try {
                    inputStream.reset();
                } catch (Exception resetEx) {
                    // 忽略重置异常
                }
            }

            return new ProxyHeaderResult(realAddress, null, inputStream);
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v2 header: {}, using original address: {}", 
                    e.getMessage(), socket.getRemoteSocketAddress(), e);
            try {
                inputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, inputStream);
        }
    }
    
    /**
     * 从已读取的字节数组中解析PROXY v2协议（备用方法）
     * @param socket 客户端Socket
     * @param signature 已读取的签名字节数组
     * @return 解析结果
     */
    private ProxyHeaderResult parseProxyV2FromBytes(Socket socket, byte[] signature, java.io.BufferedInputStream bufferedInputStream) {
        try {
            if (signature.length < 16) {
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
            }
            
            // 从signature数组中解析（假设已经读取了足够的字节）
            // 12字节: signature
            // 13字节: version/command
            int versionCommand = signature[12] & 0xFF;
            int version = (versionCommand >> 4) & 0x0F;
            int command = versionCommand & 0x0F;
            
            if (version != 2 || command != 0x01) {
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
            }
            
            // 14字节: protocol/family
            int protocolFamily = signature[13] & 0xFF;
            int family = protocolFamily & 0x0F;
            
            // 15-16字节: address length
            int addressLength = ((signature[14] & 0xFF) << 8) | (signature[15] & 0xFF);
            
            if (family == 0x01 && signature.length >= 16 + 12) {
                // IPv4
                int offset = 16;
                String clientIp = String.format("%d.%d.%d.%d",
                        signature[offset] & 0xff,
                        signature[offset + 1] & 0xff,
                        signature[offset + 2] & 0xff,
                        signature[offset + 3] & 0xff);
                int clientPort = ((signature[offset + 4] & 0xFF) << 8) | (signature[offset + 5] & 0xFF);
                
                InetSocketAddress realAddress = new InetSocketAddress(clientIp, clientPort);
                log.info("Parsed PROXY v2 from bytes, real client address: {}:{}", clientIp, clientPort);
                return new ProxyHeaderResult(realAddress, null, bufferedInputStream);
            }
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v2 from bytes: {}", e.getMessage());
        }
        return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null, bufferedInputStream);
    }
}
