package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;

public class TestAi {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("6130d6697ed4460ba78620396b3dee91.RYikV9MfkOwNThZK");
        String res = util.analyzeImage("https://img2.baidu.com/it/u=3001861759,2097332219&fm=253&app=120&size=w931&n=0&f=JPEG&fmt=auto?sec=1712595600&t=b9dbf8a706b4d30ab62b0dff1ce6aeb3", "请提取这张图片中的所有试题文字内容");
        System.out.println("Result: " + res);
    }
}
