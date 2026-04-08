package com.jlm.homework;

import com.jlm.homework.util.AIUtil;
import com.jlm.homework.util.QianWenAIUtil;
import com.jlm.homework.util.TongYiServ;
import java.lang.reflect.Field;

public class TestAiF2 {
    public static void main(String[] args) throws Exception {
        QianWenAIUtil util = new QianWenAIUtil();
        util.setAiName("qianwen");
        TongYiServ tongYiServ = new TongYiServ();
        
        Field apiKeyField = TongYiServ.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(tongYiServ, "sk-234cb378f4484f3e9f6fc5b8304508e5");
        
        Field modelField = TongYiServ.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(tongYiServ, "qwen-vl-max");
        
        util.setTongYiServ(tongYiServ);
        String res = util.analyzeText("你好");
        System.out.println("Result: " + res);
    }
}
