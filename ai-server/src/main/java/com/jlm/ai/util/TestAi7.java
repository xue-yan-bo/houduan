package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import java.io.File;

public class TestAi7 {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("6130d6697ed4460ba78620396b3dee91.RYikV9MfkOwNThZK");
        String path = new File("test_img.jpg").getAbsolutePath();
        String res = util.analyzeImage(path, "请提取这张图片中的所有试题文字内容");
        System.out.println("Result: " + res);
    }
}
