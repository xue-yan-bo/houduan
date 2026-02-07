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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ExecutorService threadPool;
    private ServerSocket serverSocket;
    private boolean running = false;

    @Value("${socket.server.port:6000}")
    private int socketPort; // 可以通过配置文件管理端口
    @Value("${socket.server.maxConnections:10}")
    private int maxConnections;


    // 连接计数器
    private final java.util.concurrent.atomic.AtomicInteger connectionCount = new java.util.concurrent.atomic.AtomicInteger(0);
    // 最大连接数
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;

    @Override
    public void start() {
        // 使用更合理的线程池大小，默认最大1000连接
        int threadPoolSize = Math.min(maxConnections, DEFAULT_MAX_CONNECTIONS);
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(socketPort);
                serverSocket.setReuseAddress(true);
                running = true;
                log.info("Socket server started on port {}", socketPort);

                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        
                        // 检查连接数是否超过限制
                        int currentConnections = connectionCount.incrementAndGet();
                        if (currentConnections > threadPoolSize) {
                            log.warn("Connection limit reached: {}, closing new connection from {}", 
                                    threadPoolSize, socket.getRemoteSocketAddress());
                            connectionCount.decrementAndGet();
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // 忽略关闭异常
                            }
                            continue;
                        }
                        
                        //log.info("New client connected, raw address: {}", socket.getRemoteSocketAddress());
                        
                        // 解析代理头，获取真实客户端地址
                        ProxyHeaderResult proxyResult;
                        try {
                            proxyResult = parseProxyHeader(socket);
                        } catch (IOException e) {
                            log.warn("Failed to parse proxy header, using default address: {}", e.getMessage());
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
                                smartDeviceUserRelationService, handlerService, sessionContext, proxyResult.getPreReadBytes(), redisTemplate, redisKey);

                        threadPool.submit(() -> {
                            try {
                                clientHandler.run();
                            } finally {
                                // 连接处理完成后，减少连接计数
                                connectionCount.decrementAndGet();
                                log.info("Client connection closed, current connections: {}", connectionCount.get());
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
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
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
        // 使用BufferedInputStream包装输入流，以便支持标记操作
        java.io.BufferedInputStream bufferedInputStream = new java.io.BufferedInputStream(inputStream, 1024);
        // 设置标记，以便重置
        bufferedInputStream.mark(1024);
        // 读取前5个字节用于检测代理头
        byte[] signature = new byte[5];
        int bytesRead = 0;

        try {
            bytesRead = bufferedInputStream.read(signature);
            
            // 默认使用Socket远程地址
            InetSocketAddress realAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

            // 更健壮的代理头处理
            if (bytesRead == -1) {
                // 流已关闭，返回默认地址
                return new ProxyHeaderResult(realAddress, null);
            } else if (bytesRead < 5) {
                // 读取字节不足，重置输入流并返回默认地址
                try {
                    bufferedInputStream.reset();
                } catch (Exception ex) {
                    // 忽略重置异常
                }
                return new ProxyHeaderResult(realAddress, null);
            } else {
                // 检测代理协议类型
                String sigStr = new String(signature);
                if (sigStr.equals("PROXY")) {
                    // PROXY v1 协议
                    return parseProxyV1(socket, bufferedInputStream);
                } else if (isProxyV2Signature(signature)) {
                    // PROXY v2 协议
                    return parseProxyV2(socket, bufferedInputStream);
                } else {
                    // 不是代理协议，重置输入流
                    try {
                        bufferedInputStream.reset();
                    } catch (Exception ex) {
                        // 忽略重置异常
                    }
                    return new ProxyHeaderResult(realAddress, null);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read proxy header signature: {}", e.getMessage());
            // 重置输入流
            try {
                bufferedInputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }
            return new ProxyHeaderResult(
                    (InetSocketAddress) socket.getRemoteSocketAddress(),
                    null
            );
        }
    }
    
    /**
     * 检测是否为PROXY v2协议签名
     * @param signature 签名字节
     * @return 是否为PROXY v2协议
     */
    private boolean isProxyV2Signature(byte[] signature) {
        return signature[0] == 0x0D && signature[1] == 0x0A && 
               signature[2] == 0x0D && signature[3] == 0x0A && 
               signature[4] == 0x00;
    }
    
    /**
     * 解析PROXY v1协议
     * @param socket 客户端Socket
     * @param inputStream 输入流
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV1(Socket socket, InputStream inputStream) throws IOException {
        try {
            // 尝试将输入流转换为BufferedInputStream，以便支持标记操作
            java.io.BufferedInputStream bufferedInputStream = null;
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
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null);
            }

            String[] parts = line.split(" ");
            if (parts.length < 6) {
                // 无效的PROXY v1格式，使用默认地址
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null);
            }
            // 获取真实客户端地址
            String clientIp = parts[2];
            int clientPort = Integer.parseInt(parts[4]);
            InetSocketAddress realAddress = new InetSocketAddress(clientIp, clientPort);

            // 再次重置输入流，以便后续的PacketDecoder能够正确读取数据
            try {
                bufferedInputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }

            return new ProxyHeaderResult(realAddress, null);
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v1 header: {}", e.getMessage());
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null);
        }
    }
    
    /**
     * 解析PROXY v2协议
     * @param socket 客户端Socket
     * @param inputStream 输入流
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV2(Socket socket, InputStream inputStream) throws IOException {
        try {
            // 尝试将输入流转换为BufferedInputStream，以便支持标记操作
            java.io.BufferedInputStream bufferedInputStream = null;
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
            // 使用DataInputStream包装BufferedInputStream
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            // 跳过PROXY v2头的前12个字节
            dataInputStream.skipBytes(12);

            // 读取客户端IP地址（IPv4，4字节）
            byte[] addressBytes = new byte[4];
            dataInputStream.readFully(addressBytes);

            // 读取客户端端口（2字节）
            int port = dataInputStream.readUnsignedShort();

            // 构建IP地址字符串
            String ip = String.format("%d.%d.%d.%d",
                    addressBytes[0] & 0xff,
                    addressBytes[1] & 0xff,
                    addressBytes[2] & 0xff,
                    addressBytes[3] & 0xff);

            // 构建真实地址
            InetSocketAddress realAddress = new InetSocketAddress(ip, port);
            
            // 再次重置输入流，以便后续的PacketDecoder能够正确读取数据
            try {
                bufferedInputStream.reset();
            } catch (Exception ex) {
                // 忽略重置异常
            }

            return new ProxyHeaderResult(realAddress, null);
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v2 header: {}", e.getMessage());
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), null);
        }
    }
}


