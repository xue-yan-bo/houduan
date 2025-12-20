package com.jlm.homework.socket.handler;

import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.protocol.Packet;

public interface MessageHandler {
    void handle(SessionContext context, Packet packet, ResponseSender sender);
}
