package com.jlm.homework.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Service
public class SocketService {
    public ServerSocket createSocket(Integer port){
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String text;

                do {
                    text = reader.readLine();
                    if (text != null) {
                        System.out.println("Received from client: " + text);
                    }
                } while (text != null);

                socket.close();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
