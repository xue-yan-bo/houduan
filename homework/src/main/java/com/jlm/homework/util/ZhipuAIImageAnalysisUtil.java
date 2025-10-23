package com.jlm.homework.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlm.homework.config.ZhipuAIConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 智谱AI图片分析工具类
 * 用于接入智谱AI的图片分析相关API
 */
public class ZhipuAIImageAnalysisUtil {

    // 智谱AI API基础URL
    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4/";
    // 图像识别API
    private static final String IMAGE_RECOGNITION_ENDPOINT = "image/analysis";
    @Value("${zhipu.ai.api-key:}")
    private String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /*public ZhipuAIImageAnalysisUtil(){
        ZhipuAIConfig config=new ZhipuAIConfig();
        config.zhipuAIImageAnalysisUtil();
    }*/
    /**
     * 构造函数
     * @param apiKey 智谱AI的API密钥
     */
    public ZhipuAIImageAnalysisUtil(String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分析图片内容
     * @param imagePath 图片文件路径
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果JSON字符串
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeImage(String imagePath, String prompt) throws IOException {
        // 1. 读取图片并进行Base64编码
        String base64Image = encodeImageToBase64(imagePath);
        
        // 2. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "glm-4.5v"); // 使用GLM-4V多模态模型
        
        // 构建messages参数
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        
        // 构建content数组，包含文本和图片
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        
        // 根据智谱AI API要求，image_url应该是一个对象而不是字符串
        Map<String, String> imageUrlObject = new HashMap<>();
        imageUrlObject.put("url", "data:image/jpeg;base64," + base64Image);
        imageContent.put("image_url", imageUrlObject);
        message.put("content", new Object[]{textContent, imageContent});
        requestBody.put("messages", new Object[]{message});
        
        // 3. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");
        
        // 4. 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "chat/completions", request, String.class);
        
        // 5. 处理响应
        return parseAndFormatResponse(response.getBody());
    }

    /**
     * 简化的图片描述生成
     * @param imagePath 图片文件路径
     * @return 图片内容描述
     * @throws IOException 文件读取或API调用异常
     */
    public String describeImage(String imagePath) throws IOException {
        String prompt = "请详细描述这张图片的内容，包括主要物体、场景、颜色、布局等信息。";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 图片中的文字识别
     * @param imagePath 图片文件路径
     * @return 识别的文字内容
     * @throws IOException 文件读取或API调用异常
     */
    public String recognizeTextInImage(String imagePath) throws IOException {
        String prompt = "请识别图片中所有的文字内容，并以文本形式返回。";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 图片中的试题分析
     * @param imagePath 图片文件路径
     * @return 识别的文字内容
     * @throws IOException 文件读取或API调用异常
     */
    public String recognizeTitleInImage(String imagePath) throws IOException {
        String prompt = "请识别分析图片中所有试题的内容，并以文本形式返回，包括大题号、小题号、题内容、题类型、考察知识点、答题是否正确、参考答案、解析。";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 图片中的试题批阅
     * @param imagePath 图片文件路径
     * @return 识别的文字内容
     * @throws IOException 文件读取或API调用异常
     */
    public String recognizePiyueInImage(String imagePath) throws IOException {
        String prompt = "请识别批阅图片中所有试题的内容，并以文本形式返回 包括大题号（包含题型）、小题号、批阅结果（批阅结果可以是 正确、错误、未答题），不要题内容。";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 图片中的试题-批阅
     * @param imagePath 图片文件路径
     * @return 识别的文字内容
     * @throws IOException 文件读取或API调用异常
     */
    public String auditTitleInImage(String imagePath) throws IOException {
        String prompt = "请批阅图片中所有试题，并以图片格式保存，返回批阅后的图片路径";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 图片中的物体检测
     * @param imagePath 图片文件路径
     * @return 检测到的物体列表及其位置
     * @throws IOException 文件读取或API调用异常
     */
    public String detectObjectsInImage(String imagePath) throws IOException {
        String prompt = "请识别图片中的主要物体，并尽可能详细地描述它们的位置、大小和特征。";
        return analyzeImage(imagePath, prompt);
    }

    /**
     * 将图片编码为Base64字符串
     * @param imagePath 图片路径（支持本地文件路径或HTTP URL）
     * @return Base64编码后的图片数据
     * @throws IOException 读取异常
     */
    private String encodeImageToBase64(String imagePath) throws IOException {
        // 规范化URL格式，将反斜杠替换为正斜杠，确保http://格式正确
        String normalizedPath = imagePath;
        if(!normalizedPath.startsWith("http://") && !normalizedPath.startsWith("https://")) {
            normalizedPath = imagePath.replace("\\", "/")
                    .replace("http:/", "http://");
        }
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            // 处理网络图片
            URL url = new URL(normalizedPath);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                return Base64Utils.encodeToString(bytes);
            }
        } else {
            // 处理本地文件
            File file = new File(normalizedPath);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                return Base64Utils.encodeToString(bytes);
            }
        }
    }

    /**
     * 解析和格式化API响应
     * @param responseBody API返回的原始响应
     * @return 格式化后的响应结果
     * @throws IOException JSON解析异常
     */
    private String parseAndFormatResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        // 检查是否有error字段
        if (rootNode.has("error")) {
            JsonNode errorNode = rootNode.get("error");
            String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "未知错误";
            throw new IOException("API调用失败: " + errorMessage);
        }
        
        // 提取choices中的内容
        if (rootNode.has("choices") && !rootNode.get("choices").isEmpty()) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                return firstChoice.get("message").get("content").asText();
            }
        }
        
        // 如果没有找到预期的内容格式，返回原始响应
        return responseBody;
    }

    /**
     * 创建带配置参数的工具实例
     * @param apiKey 智谱AI的API密钥
     * @return 工具实例
     */
    public static ZhipuAIImageAnalysisUtil createInstance(String apiKey) {
        return new ZhipuAIImageAnalysisUtil(apiKey);
    }

    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        try {
            // 替换为您的实际API密钥
            String apiKey = "7abc333508dd4d71b83dcb6f5a511eee.rjwsPZyUfDN5abhn";
            ZhipuAIImageAnalysisUtil util = new ZhipuAIImageAnalysisUtil(apiKey);
            
            // 替换为实际的图片路径
            // 替换为实际的图片路径
            String imagePath = "E:\\jlm\\jlm-server-finally\\10月21日数学作业二_李强_1页作业.png";
            // 规范化URL路径
            String normalizedPath = imagePath.replace("\\", "/")
                    .replace("http:/", "http://");

            // 测试图片描述
            //String description = util.describeImage(normalizedPath);
            //System.out.println("图片描述: " + description);
            
            // 测试文字识别
            //String text = util.recognizeTextInImage(imagePath);
            //System.out.println("识别的文字: " + text);


            //试题识别
            String titleText = util.recognizePiyueInImage(imagePath);
            System.out.println("试题内容: " + titleText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}