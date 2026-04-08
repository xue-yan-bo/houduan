package com.jlm.ai.util;

import com.jlm.ai.util.ZhipuAIImageAnalysisUtil;
import java.io.File;
import java.util.Base64;
import java.nio.file.Files;

public class TestAi5 {
    public static void main(String[] args) throws Exception {
        ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil("6130d6697ed4460ba78620396b3dee91.RYikV9MfkOwNThZK");
        String path = new File("test_img.jpg").getAbsolutePath();
        
        byte[] fileContent = Files.readAllBytes(new File(path).toPath());
        String base64Image = Base64.getEncoder().encodeToString(fileContent);
        System.out.println("Base64 string starts with: " + base64Image.substring(0, 50));
        System.out.println("Base64 string length: " + base64Image.length());
    }
}
