package com.jlm.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class TestAi9 {
    public static void main(String[] args) throws Exception {
        Map<String, String> imageUrlObject = new HashMap<>();
        imageUrlObject.put("url", "data:image/jpeg;base64,1234");
        
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", imageUrlObject);
        
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(imageContent));
    }
}
