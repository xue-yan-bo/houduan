package com.jlm.homework.socket;

import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SocketService implements SmartLifecycle {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;

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
                    //System.out.println("New client connected");
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    log.info("New client connected from: {}", clientAddress);
                    // 提交客户端连接到线程池处理
                    threadPool.submit(new ClientHandler(socket,messagingTemplate,
                            smartDeviceUserRelationService,studentsHomeworkNewService));
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

}


