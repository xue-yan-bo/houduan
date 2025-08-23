package com.jlm.homework.controller;

import com.jlm.homework.util.SseManagerUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;

/**
 * @author jlm
 */
@Tag(name = "SSE链接", description = "SSE的创建链接、发送消息、关闭连接、获取客户ID等操作")
@RestController
@RequestMapping("/api/sse")
public class SseController {

    @Resource
    private SseManagerUtil sseManagerUtil;


    @CrossOrigin
    @GetMapping("/createSseConnect")
    public SseEmitter createSseConnect(String clientId){
        return sseManagerUtil.createSseConnect(clientId);
    }

    @CrossOrigin
    @GetMapping("/sendMsg")
    public String sendMsg(String clientId, String msg){
        return sseManagerUtil.sendMsgToClient(clientId, msg);
    }

    @CrossOrigin
    @GetMapping("/closeConnect")
    public String closeConnect(String clientId) {
        sseManagerUtil.closeSseConnect(clientId);
        return "ok";
    }

    @GetMapping("/getClient")
    public List<String> getClient() {
        return sseManagerUtil.getClients();
    }


}
