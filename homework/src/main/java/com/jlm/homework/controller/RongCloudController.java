package com.jlm.homework.controller;

import com.alibaba.fastjson.JSONObject;
import com.jlm.homework.dto.RongMsgResult;
import com.jlm.homework.util.RongCloudUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "融云", description = "融云的查询token、发送消息、用户、历史记录等操作")
@RestController
@RequestMapping("/api/rongCloud")
public class RongCloudController {
    @PostMapping("/get-rongcloud-token")
    public String getToken(String userId, String userHead, String userName) {
        return RongCloudUtil.getToken(userId,userHead,userName);
    }

    @PostMapping("/send-msg")
    public RongMsgResult publishMsg(String fromUserId, String toUserId, String objectName, String content) {
        return RongCloudUtil.publishMsg(fromUserId,toUserId,objectName,content);
    }

    @PostMapping("/userInfo")
    public JSONObject userInfo(String userId) {
        return RongCloudUtil.userInfo(userId);
    }

    @PostMapping("/history-msg")
    public JSONObject getHistoryMsg(String date) {
        return RongCloudUtil.getHistoryMsg(date);
    }
}
