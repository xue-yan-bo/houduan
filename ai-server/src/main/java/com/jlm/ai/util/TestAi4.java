package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import java.io.File;

public class TestAi4 {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("6130d6697ed4460ba78620396b3dee91.RYikV9MfkOwNThZK");
        String res = util.analyzeImage("https://img0.baidu.com/it/u=2273614948,3004812368&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1712595600&t=f64c67674ffedee48281358055ee1e7b", "请提取这张图片中的所有试题文字内容");
        System.out.println("Result: " + res);
    }
}
