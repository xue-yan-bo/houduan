package com.jlm.homework.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@Tag(name = "语音聊天", description = "语音聊天")
@ServerEndpoint("/voiceChat")
public class ChatServer {
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static Set<String> messageFiles = Collections.synchronizedSet(new HashSet<>());
    private static final String MESSAGE_DIR = "messages";
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        try {
            session.getBasicRemote().sendText("欢迎加入聊天！");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("onOpen");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("onMessage");
    }
    @OnMessage
    public void onMessage(byte[] message, Session userSession) throws IOException {
        Path dirPath = Paths.get(MESSAGE_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String fileName = "message_" + System.currentTimeMillis() + ".wav";
        Path filePath = Paths.get(MESSAGE_DIR, fileName);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(message);
        }

        messageFiles.add(fileName);
        broadcastMessageList(userSession);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("onClose");
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    private static void broadcast(String message) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void broadcastMessageList(Session userSession) throws IOException {
        StringBuilder fileListBuilder = new StringBuilder();
        for (String file : messageFiles) {
            fileListBuilder.append(file).append(",");
        }
        String fileList = fileListBuilder.toString();

        for (Session session : userSession.getOpenSessions()) {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(fileList);
            }
        }
    }
    public static byte[] getMessage(String fileName) throws IOException {
        Path filePath = Paths.get(MESSAGE_DIR, fileName);
        return Files.readAllBytes(filePath);
    }
}
