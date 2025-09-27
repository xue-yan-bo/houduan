/*
package com.jlm.homework.controller;

import jakarta.websocket.*;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class ChatClient {
    private static final String SERVER_URI = "ws://localhost:8080/chat";

    private Session session;
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
    }
    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
    }

    public static void main(String[] args) throws DeploymentException, IOException {
        WebSocketContainer container =  ContainerProvider.getWebSocketContainer();
        URI uri = URI.create(SERVER_URI);
        Session session = container.connectToServer(ChatClient.class,uri);
        ChatClient client = new ChatClient();
        client.sendMessage("Hello World");
        session.close();

    }
}
*/
