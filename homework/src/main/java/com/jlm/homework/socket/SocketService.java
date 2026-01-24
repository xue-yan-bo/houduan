package com.jlm.homework.socket;

import com.jlm.homework.service.IHandlerService;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.socket.context.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SocketService implements SmartLifecycle {
    private static Map<String, SessionContext> clientAddressMap = new HashMap<>();
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
                    InetSocketAddress realAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
                    String clientAddress =realAddress.getHostString();
                    log.info("New client connected from: {}", clientAddress);
                    SessionContext sessionContext =null;
                    if(clientAddressMap.containsKey(clientAddress)){
                        sessionContext = clientAddressMap.get(clientAddress);
                    }else{
                        sessionContext = new  SessionContext();
                        clientAddressMap.put(clientAddress,sessionContext);
                    }
                    // 提交客户端连接到线程池处理
                    ClientHandler clientHandler=new ClientHandler(socket,messagingTemplate,
                            smartDeviceUserRelationService,handlerService,sessionContext);

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


}


