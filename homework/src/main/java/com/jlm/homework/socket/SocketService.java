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


    @Override
    public void start() {
        threadPool = Executors.newFixedThreadPool(maxConnections);
        new Thread(() -> {
            try {

                serverSocket = new ServerSocket(socketPort);
                running = true;
                //System.out.println("Socket server started on port " + socketPort);

                while (running) {
                    Socket socket = serverSocket.accept();
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
                    
                    log.info("Client connected with real address: {}", realAddress);
                    
                    // 使用Redis获取或创建SessionContext
                    SessionContext sessionContext = null;
                    String redisKey = REDIS_KEY_PREFIX + clientAddress;
                    
                    // 从Redis获取SessionContext
                    Object sessionObj = redisTemplate.opsForValue().get(redisKey);
                    if (sessionObj instanceof SessionContext) {
                        sessionContext = (SessionContext) sessionObj;
                        //log.info("Found existing session context in Redis for client: {}", clientAddress);
                    } else {
                        // 创建新的SessionContext
                        sessionContext = new SessionContext();
                        // 保存到Redis，设置过期时间为24小时
                        redisTemplate.opsForValue().set(redisKey, sessionContext, 24, java.util.concurrent.TimeUnit.HOURS);
                        //log.info("Created new session context and saved to Redis for client: {}", clientAddress);
                    }
                    
                    // 设置真实客户端地址到SessionContext
                    sessionContext.setRemoteAddress(realAddress);
                    sessionContext.setClientIP(clientAddress);
                    sessionContext.setClientPort(clientPort);
                    
                    // 提交客户端连接到线程池处理
                    ClientHandler clientHandler = new ClientHandler(socket, messagingTemplate,
                            smartDeviceUserRelationService, handlerService, sessionContext, proxyResult.getPreReadBytes());

                    threadPool.submit(clientHandler);
                }
            } catch (IOException e) {
                System.err.println("Socket server error: " + e.getMessage());
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

        // 直接使用原始输入流，不包装为 BufferedInputStream
        // 读取前5个字节用于检测代理头
        byte[] signature = new byte[5];
        int bytesRead = 0;

        // 使用临时缓冲区保存读取的数据
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            bytesRead = inputStream.read(signature);
            // 将读取的字节保存到临时缓冲区
            baos.write(signature, 0, bytesRead);
        } catch (Exception e) {
            log.warn("Failed to read proxy header signature: {}", e.getMessage());
            return new ProxyHeaderResult(
                    (InetSocketAddress) socket.getRemoteSocketAddress(),
                    baos.toByteArray()
            );
        }

        // 默认使用Socket远程地址
        InetSocketAddress realAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        byte[] preReadBytes = baos.toByteArray();

        // 更健壮的代理头处理
        if (bytesRead == -1) {
            // 流已关闭，返回默认地址
            return new ProxyHeaderResult(realAddress, preReadBytes);
        } else if (bytesRead < 5) {
            // 读取字节不足，返回默认地址
            return new ProxyHeaderResult(realAddress, preReadBytes);
        } else {
            // 检测代理协议类型
            String sigStr = new String(signature);
            if (sigStr.equals("PROXY")) {
                // PROXY v1 协议
                return parseProxyV1(socket, inputStream, preReadBytes);
            } else if (isProxyV2Signature(signature)) {
                // PROXY v2 协议
                return parseProxyV2(socket, inputStream, preReadBytes);
            } else {
                // 不是代理协议，返回默认地址
                return new ProxyHeaderResult(realAddress, preReadBytes);
            }
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
     * @param preReadBytes 预读取的字节
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV1(Socket socket, InputStream inputStream, byte[] preReadBytes) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            if (line == null || !line.startsWith("PROXY")) {
                // 无效的PROXY v1头，使用默认地址
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), preReadBytes);
            }

            String[] parts = line.split(" ");
            if (parts.length < 6) {
                // 无效的PROXY v1格式，使用默认地址
                return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), preReadBytes);
            }
            // 获取真实客户端地址
            String clientIp = parts[2];
            int clientPort = Integer.parseInt(parts[4]);
            InetSocketAddress realAddress = new InetSocketAddress(clientIp, clientPort);

            // 读取剩余的代理头数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (preReadBytes != null) {
                baos.write(preReadBytes);
            }
            // 读取

            byte[] crlf = new byte[2];
            int crlfRead = inputStream.read(crlf);
            if (crlfRead > 0) {
                baos.write(crlf, 0, crlfRead);
            }

            return new ProxyHeaderResult(realAddress, baos.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v1 header: {}", e.getMessage());
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), preReadBytes);
        }
    }
    
    /**
     * 解析PROXY v2协议
     * @param socket 客户端Socket
     * @param inputStream 输入流
     * @param preReadBytes 预读取的字节
     * @return 解析结果
     * @throws IOException 解析异常
     */
    private ProxyHeaderResult parseProxyV2(Socket socket, InputStream inputStream, byte[] preReadBytes) throws IOException {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);

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
            
            // 读取剩余的代理头数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (preReadBytes != null) {
                baos.write(preReadBytes);
            }
            // 读取剩余的代理头字节
            byte[] remaining = new byte[108 - 20]; // 假设代理头最大长度为108字节
            int remainingRead = dataInputStream.read(remaining);
            if (remainingRead > 0) {
                baos.write(remaining, 0, remainingRead);
            }

            return new ProxyHeaderResult(realAddress, baos.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to parse PROXY v2 header: {}", e.getMessage());
            return new ProxyHeaderResult((InetSocketAddress) socket.getRemoteSocketAddress(), preReadBytes);
        }
    }
}


