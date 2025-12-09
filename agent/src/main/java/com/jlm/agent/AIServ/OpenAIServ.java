package com.jlm.agent.AIServ;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.*;

/**
 * 千问
 */
@Component
public class OpenAIServ {

    @Resource
    private OpenAiChatModel chatModel;

    /**
     * @param userText 提示词
     * @param medias     上传的文件
     * @return
     */
    public String multiModalCall(String userText, List<Media> medias) {
        // 参数校验
        if (userText == null || userText.equals("")) {
            return "";
        }
        UserMessage userMessage = UserMessage.builder()
                .text(userText)
                .media(medias)
                .build();

        String response = chatModel.call(userMessage);
        return response;
    }



}


