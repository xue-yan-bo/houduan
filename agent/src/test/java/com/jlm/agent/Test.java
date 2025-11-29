package com.jlm.agent;

import jakarta.annotation.Resource;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Test {

    @Resource
    ZhiPuAiChatModel zhiPuAiChatModel;


    @org.junit.jupiter.api.Test
    void test(){
        String string = zhiPuAiChatModel.call("你好");
        System.out.println(string);
    }


}
