package com.jlm.agent.AIServ;// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.*;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson2.JSON;
import com.jlm.agent.domain.TopicReportEnt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class TongYiSDKServ {

    @Value("${TONGYI_API_KEY:}")
    private String apiKey;
    @Value("${TONGYI_MODEL:}")
    private String model;

    /**
     *
     * @param text  提示词
     * @param imageUrls 外网url或图片base64，注意图片base64格式需要加前缀，如：data:image/jpeg;base64,/9j/......
     */
    public TopicReportEnt multiModalCall(String systemText,String userText, List<String> imageUrls) {

        // 入参组装
        ArrayList userParaList = new ArrayList();
        userParaList.add(Collections.singletonMap("text", userText));
        if (!CollectionUtils.isEmpty(imageUrls)) {
            for (String imageUrl : imageUrls) {
                userParaList.add(Collections.singletonMap("image", imageUrl));
            }
        }

        ArrayList sysParaList = new ArrayList();
        sysParaList.add(Collections.singletonMap("text", systemText));


        // 调用接口
        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(userParaList).build();
        MultiModalMessage sysMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                .content(sysParaList).build();


        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                // 此处以qwen-vl-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(model)
                .message(sysMessage)
                .message(userMessage)
                .build();
        MultiModalConversationResult result = null;
        try {
            result = conv.call(param);
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            throw new RuntimeException(e);
        }
        List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
        String text = content.get(0).get("text").toString();


        String cleanJson = text
                .replaceAll("^\\s*```(?:json)?", "") // 去掉开头的 ``` 或 ```json
                .replaceAll("\\s*```\\s*$", "")      // 去掉结尾的 ```
                .replaceAll("\\\\", "")
                .trim();;



        TopicReportEnt topicReportEnt = JSON.parseObject(cleanJson, TopicReportEnt.class);

        return topicReportEnt;
    }


}
