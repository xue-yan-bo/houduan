package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import java.io.File;

public class TestAi10 {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("6130d6697ed4460ba78620396b3dee91.RYikV9MfkOwNThZK");
        String res = util.analyzeText("hello");
        System.out.println("Result: " + res);
    }
}
