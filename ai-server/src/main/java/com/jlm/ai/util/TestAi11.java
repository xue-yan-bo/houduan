package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import java.io.File;

public class TestAi11 {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("22d592c665f749799d69f2b0c15233db.jw1mkeTjslTeGtJf");
        String res = util.analyzeText("hello");
        System.out.println("Result: " + res);
    }
}
