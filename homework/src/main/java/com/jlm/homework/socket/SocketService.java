package com.jlm.homework.socket;

import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


@Component
public class SocketService implements SmartLifecycle {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ISmartDeviceUserRelationService smartDeviceUserRelationService;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    private ServerSocket serverSocket;
    private boolean running = false;
    
    @Value("${socket.server.port:6000}")
    private int socketPort; // 可以通过配置文件管理端口



    @Override
    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(socketPort);
                running = true;
                System.out.println("Socket server started on port " + socketPort);
                
                while (running) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected");
                    
                    // 创建新线程处理连接
                    Thread thread = new Thread(new ClientHandler(socket,messagingTemplate,
                            smartDeviceUserRelationService,studentsHomeworkNewService));
                    thread.start();
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


}


