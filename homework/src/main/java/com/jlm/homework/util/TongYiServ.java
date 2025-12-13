package com.jlm.homework.util;// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class TongYiServ {

    @Value("${TONGYI_API_KEY}")
    private String apiKey;
    @Value("${TONGYI_MODEL}")
    private String model;

    /**
     *
     * @param text  提示词
     * @param imageUrls 外网url或图片base64，注意图片base64格式需要加前缀，如：data:image/jpeg;base64,/9j/......
     */
    public String multiModalCall(String text, List<String> imageUrls) {

        // 参数校验
        if (text == null || text.equals("")) {
            return "";
        }

        // 入参组装
        ArrayList paraList = new ArrayList();
        paraList.add(Collections.singletonMap("text", text));
        if (!CollectionUtils.isEmpty(imageUrls)) {
            for (String imageUrl : imageUrls) {
                // 检查是否是有效的URL，如果不是，可能是本地文件路径
                if (isValidUrl(imageUrl)) {
                    paraList.add(Collections.singletonMap("image", imageUrl));
                } else {
                    // 对于本地文件路径，将其转换为base64编码格式
                    try {
                        String base64Image = encodeFileToBase64Binary(imageUrl);
                        // 获取文件扩展名
                        String fileExtension = getFileExtension(imageUrl);
                        // 构建base64图片格式：data:image/{extension};base64,{base64Data}
                        String base64Format = "data:image/" + fileExtension + ";base64," + base64Image;
                        paraList.add(Collections.singletonMap("image", base64Format));
                    } catch (Exception e) {
                        // 如果转换失败，记录错误并尝试直接使用原路径
                        System.err.println("Failed to convert file to base64: " + e.getMessage());
                        paraList.add(Collections.singletonMap("image", imageUrl));
                    }
                }
            }
        }

        // 调用接口
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(paraList).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                // 此处以qwen-vl-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(model)
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
        return content.get(0).get("text").toString();
    }
    
    // 用于判断是否是有效的URL的辅助方法
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    // 将文件转换为base64编码的辅助方法
    private String encodeFileToBase64Binary(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileContent = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(fileContent);
        }
        return Base64.getEncoder().encodeToString(fileContent);
    }
    
    // 获取文件扩展名的辅助方法
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 如果没有扩展名，默认使用jpeg
            return "jpeg";
        }
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }


}
