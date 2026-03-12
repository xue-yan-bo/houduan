package com.jlm.homework.socket.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


// 消息中心类
public class MessageCenter {
    private static final Map<String, Set<Channel>> topicChannels = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 订阅主题
    public void subscribe(String topic, Channel channel) {
        topicChannels.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(channel);
    }

    // 取消订阅
    public void unsubscribe(String topic, Channel channel) {
        Set<Channel> channels = topicChannels.get(topic);
        if (channels != null) {
            channels.remove(channel);
        }
    }

    // 发送消息到主题
    public void sendToTopic(String topic, Object message) throws JsonProcessingException {
        Set<Channel> channels = topicChannels.get(topic);
        if (channels != null) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextWebSocketFrame frame = new TextWebSocketFrame(jsonMessage);
            for (Channel channel : channels) {
                if (channel.isActive()) {
                    channel.writeAndFlush(frame);
                }
            }
        }
    }
}
