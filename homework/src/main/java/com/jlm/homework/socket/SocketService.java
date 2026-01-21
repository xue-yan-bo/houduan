package com.jlm.homework.socket;

import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SocketService implements SmartLifecycle {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;
    @Autowired
    private IHandlerService handlerService;

    private ExecutorService threadPool;
    private ServerSocket serverSocket;
    private boolean running = false;
    
    @Value("${socket.server.port:6000}")
    private int socketPort; // 可以通过配置文件管理端口
    @Value("${socket.server.maxConnections:10}")
    private int maxConnections;
    
    // 保存客户端标识符到线程的映射，用于后续连接复用同一线程
    private final ConcurrentHashMap<String, Integer> clientThreadMap = new ConcurrentHashMap<>();
    // 自定义线程池，使用固定线程数
    private WorkerThread[] workerThreads;
    // 客户端连接队列
    private BlockingQueue<ClientHandler>[] clientQueues;
    // 原子整数，用于生成线程ID
    private final AtomicInteger nextThreadId = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    private void initializeClientQueues() {
        // 初始化客户端队列数组，使用maxConnections作为大小
        clientQueues = (BlockingQueue<ClientHandler>[]) new BlockingQueue[maxConnections];
        for (int i = 0; i < maxConnections; i++) {
            clientQueues[i] = new LinkedBlockingQueue<>();
        }
    }

    @Override
    public void start() {
        // 初始化客户端队列
        initializeClientQueues();
        
        // 初始化工作线程
        workerThreads = new WorkerThread[maxConnections];
        for (int i = 0; i < maxConnections; i++) {
            workerThreads[i] = new WorkerThread(i, clientQueues[i]);
            workerThreads[i].start();
        }
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(socketPort);
                running = true;
                log.info("Socket server started on port {}", socketPort);
                
                while (running) {
                    Socket socket = serverSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    int clientPort = socket.getPort();
                    // 使用客户端IP+端口作为唯一标识符
                    String clientId = clientAddress + ":" + clientPort;
                    
                    log.info("New client connected from: {}", clientId);
                    
                    // 创建客户端处理器
                    ClientHandler clientHandler = new ClientHandler(socket, messagingTemplate,
                            smartDeviceUserRelationService, handlerService);
                    
                    // 获取或分配线程ID
                    Integer threadId = clientThreadMap.computeIfAbsent(clientId, key -> {
                        // 使用哈希算法分配线程，确保同一客户端始终分配到同一线程
                        int hash = Math.abs(key.hashCode() % maxConnections);
                        log.info("Assigning client {} to thread {}", key, hash);
                        return hash;
                    });
                    
                    // 将客户端处理器放入对应线程的队列
                    clientQueues[threadId].put(clientHandler);
                }
            } catch (IOException | InterruptedException e) {
                log.error("Socket server error: {}", e.getMessage(), e);
            }
        }).start();
    }
    
    // 工作线程类，负责处理客户端连接
    private static class WorkerThread extends Thread {
        private final int threadId;
        private final BlockingQueue<ClientHandler> clientQueue;
        
        public WorkerThread(int threadId, BlockingQueue<ClientHandler> clientQueue) {
            super("SocketWorker-" + threadId);
            this.threadId = threadId;
            this.clientQueue = clientQueue;
        }
        
        @Override
        public void run() {
            try {
                while (true) {
                    // 从队列中获取客户端处理器并执行
                    ClientHandler clientHandler = clientQueue.take();
                    try {
                        clientHandler.run();
                    } catch (Exception e) {
                        log.error("Error handling client on thread {}: {}", threadId, e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker thread {} interrupted", threadId);
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket: {}", e.getMessage(), e);
        }
        
        // 中断所有工作线程
        if (workerThreads != null) {
            for (WorkerThread workerThread : workerThreads) {
                workerThread.interrupt();
            }
        }
        
        // 清空客户端线程映射
        clientThreadMap.clear();
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
    
    // 移除客户端线程映射，当客户端断开连接时调用
    public void removeClientThreadMapping(String clientId) {
        clientThreadMap.remove(clientId);
    }

}


