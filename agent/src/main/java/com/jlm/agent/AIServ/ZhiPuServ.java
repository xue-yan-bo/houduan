package com.jlm.agent.AIServ;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 智谱
 */

@Component
public class ZhiPuServ {

    @Autowired
    private ZhiPuAiChatModel zhiPuAiChatModel;

    /**
     * 多模态调用接口
     * @param text
     * @param medias
     * @return
     */
    public String multiModalCall(String text, List<Media> medias) {

        // 参数校验
        if (text == null || text.equals("")) {
            return "";
        }

        UserMessage userMessage = UserMessage.builder()
        .text(text)
        .media(medias)
        .build();

        String callstr= zhiPuAiChatModel.call(userMessage);
        return callstr;
    }
}
