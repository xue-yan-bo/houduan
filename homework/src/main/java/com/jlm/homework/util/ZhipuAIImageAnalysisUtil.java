package com.jlm.homework.util;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.core.Constants;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import ai.z.openapi.service.image.ImageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.entity.ExamPaperAnalysis;
import com.jlm.homework.entity.QuestionAnalysis;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    }
    


    
    /**
     * 生成试卷分析专用提示词
     * @param currentPage 当前页码
     * @param totalPages 总页数
     * @return 针对试卷分析的优化提示词
     */
    public String generatePaperAnalysisPrompt(int currentPage, int totalPages) {
        return "请详细分析这张试卷页面（第" + currentPage + "页，共" + totalPages + "页）的内容。" +
               "请识别并提取以下信息：\n" +
               "1. 所有试题的题号、题目内容、题型\n" +
               "2. 题目中的条件、要求和分值\n" +
               "3. 答题区域的内容\n" +
               "4. 已有的批改痕迹（如有）\n" +
               "请尽量保持原始内容的准确性，不要遗漏任何试题信息。";
    }
    
    /**
     * 分析整份试卷（由多张图片组成）
     * @param paperImagePaths 试卷图片路径列表（按页码顺序排列）
     * @return 整份试卷的综合分析结果
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeExamPaper(List<String> paperImagePaths) throws IOException {
        if (paperImagePaths == null || paperImagePaths.isEmpty()) {
            throw new IllegalArgumentException("试卷图片列表不能为空");
        }
        
        // 1. 首先批量分析每张试卷图片
        Map<String, String> pageResults = batchAnalyzeImages(paperImagePaths, generatePaperAnalysisPrompt(1, paperImagePaths.size()));
        
        // 2. 整理所有页面的分析结果，按原始顺序
        StringBuilder allPagesContent = new StringBuilder();
        for (int i = 0; i < paperImagePaths.size(); i++) {
            String imagePath = paperImagePaths.get(i);
            String pageResult = pageResults.get(imagePath);
            if (pageResult != null) {
                allPagesContent.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                allPagesContent.append(pageResult).append("\n\n");
            }
        }
        
        // 3. 发送整合请求，让AI将所有页面结果合并为一份完整的试卷分析
        String integrationPrompt = generateIntegrationPrompt(allPagesContent.toString());
        return analyze(integrationPrompt);
    }
    
    /**
     * 异步分析整份试卷
     * @param paperImagePaths 试卷图片路径列表（按页码顺序排列）
     * @return 包含整份试卷分析结果的CompletableFuture
     */
    public CompletableFuture<String> analyzeExamPaperAsync(List<String> paperImagePaths) {
        if (paperImagePaths == null || paperImagePaths.isEmpty()) {
            return CompletableFuture.completedFuture("试卷图片列表不能为空");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeExamPaper(paperImagePaths);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }
    

    
    /**
     * 生成试卷结果整合提示词
     * @param allPagesContent 所有页面的分析结果
     * @return 用于整合分析的提示词
     */
    private String generateIntegrationPrompt(String allPagesContent) {
        return "以下是一份试卷所有页面的分析结果，请将它们整合成一份完整的试卷分析报告：\n\n" +
               allPagesContent + "\n\n" +
               "请按照以下格式整合：\n" +
               "1. 试卷概览：包括试卷名称、总页数、总题数、总分值等基本信息\n" +
               "2. 试题列表：按照题号顺序排列所有试题，标明题型、分值、考察知识点\n" +
               "3. 答题分析：对学生的答题情况进行整体评价\n" +
               "4. 批改情况：如有批改痕迹，请汇总批改结果\n" +
               "5. 建议：根据试卷内容提供教学或学习建议\n\n" +
               "请确保整合后的报告逻辑清晰，内容连贯，将不同页面的信息无缝衔接。";
    }
    
    /**
     * 分析试卷并生成详细的评分报告
     * @param paperImagePaths 试卷图片路径列表（按页码顺序排列）
     * @return 包含评分的详细报告
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeExamPaperWithGrading(List<String> paperImagePaths) throws IOException {
        if (paperImagePaths == null || paperImagePaths.isEmpty()) {
            throw new IllegalArgumentException("试卷图片列表不能为空");
        }
        
        // 使用特殊的评分提示词
        Map<String, String> pageResults = new HashMap<>();
        for (int i = 0; i < paperImagePaths.size(); i++) {
            String imagePath = paperImagePaths.get(i);
            String gradingPrompt = "请详细分析这张试卷页面（第" + (i + 1) + "页，共" + paperImagePaths.size() + "页）。" +
                                  "请对每个题目进行评分，包括：\n" +
                                  "1. 题目信息：题号、题型、分值\n" +
                                  "2. 学生答案分析\n" +
                                  "3. 得分情况（按步骤或要点）\n" +
                                  "4. 错误分析（如有）\n" +
                                  "5. 改进建议";
            pageResults.put(imagePath, analyzeImage(imagePath, gradingPrompt));
        }
        
        // 整理结果并生成最终评分报告
        StringBuilder allPagesGrading = new StringBuilder();
        for (int i = 0; i < paperImagePaths.size(); i++) {
            String imagePath = paperImagePaths.get(i);
            String gradingResult = pageResults.get(imagePath);
            if (gradingResult != null) {
                allPagesGrading.append("=== 第 ").append(i + 1).append(" 页评分 ===\n");
                allPagesGrading.append(gradingResult).append("\n\n");
            }
        }
        
        // 请求生成最终评分报告
        String finalGradingPrompt = "请根据以下各页面的评分结果，生成一份完整的试卷评分报告：\n\n" +
                                   allPagesGrading + "\n\n" +
                                   "请包含：\n" +
                                   "1. 总体得分和评分等级\n" +
                                   "2. 各大题得分情况统计\n" +
                                   "3. 知识掌握情况分析\n" +
                                   "4. 主要错误类型总结\n" +
                                   "5. 详细的改进建议\n\n" +
                                   "请确保报告全面、客观、有建设性。";
        
        return analyze(finalGradingPrompt);
    }
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
     * 批量分析多张试卷图片
     * @param imagePaths 图片文件路径列表
     * @param prompt 提示词，指导模型如何分析图片
     * @return 图片路径与分析结果的映射
     * @throws IOException 文件读取或API调用异常
     */
    public Map<String, String> batchAnalyzeImages(List<String> imagePaths, String prompt) throws IOException {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new HashMap<>();
        }
        
        // 创建线程池用于并行处理图片
        int threadCount = Math.min(5, imagePaths.size()); // 限制最大线程数
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        Map<String, Future<String>> futureResults = new HashMap<>();
        
        try {
            // 提交所有图片分析任务
            for (String imagePath : imagePaths) {
                final String finalImagePath = imagePath;
                futureResults.put(imagePath, executorService.submit(() -> analyzeSingleImage(finalImagePath, prompt)));
            }
            
            // 收集所有结果
            Map<String, String> results = new HashMap<>();
            for (Map.Entry<String, Future<String>> entry : futureResults.entrySet()) {
                String imagePath = entry.getKey();
                try {
                    String result = entry.getValue().get(); // 获取分析结果，会阻塞直到完成
                    results.put(imagePath, result);
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("分析图片失败: " + imagePath, e);
                }
            }
            
            return results;
        } finally {
            executorService.shutdown();
        }
    }
    
    /**
     * 批量分析多张试卷图片 - 异步版本
     * @param imagePaths 图片文件路径列表
     * @param prompt 提示词，指导模型如何分析图片
     * @return 包含分析结果的CompletableFuture
     */
    public CompletableFuture<Map<String, String>> batchAnalyzeImagesAsync(List<String> imagePaths, String prompt) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        
        // 为每个图片创建CompletableFuture
        List<CompletableFuture<AbstractMap.SimpleEntry<String, String>>> futures = imagePaths.stream()
                .map(imagePath -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String result = analyzeSingleImage(imagePath, prompt);
                        return new AbstractMap.SimpleEntry<>(imagePath, result);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }))
                .collect(Collectors.toList());
        
        // 组合所有结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    
    /**
     * 内部方法：分析单张图片（供批量处理调用）
     * @param imagePath 图片文件路径
     * @param prompt 提示词
     * @return 分析结果
     * @throws IOException 异常
     */
    private String analyzeSingleImage(String imagePath, String prompt) throws IOException {
        return analyzeImage(imagePath, prompt);
    }
    
    /**
     * 分析图片内容
     * @param imagePath 图片文件路径
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果文本
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
     * 分析图片内容（使用CogView-4模型）
     * @param imagePath 图片文件路径
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果文本
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeImage2(String imagePath, String prompt) throws IOException {
        // 1. 读取图片并进行Base64编码
        String base64Image = encodeImageToBase64(imagePath);

        // 2. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "CogView-4"); // 使用GLM-4V多模态模型

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
     * 分析问题内容
     * @param prompt 提示词，指导模型如何分析
     * @return 分析结果文本
     * @throws IOException API调用异常
     */
    public String analyze(String prompt) throws IOException {


        // 1. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "glm-4.5v"); // 使用GLM-4V多模态模型
        // 构建messages参数
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        // 构建content数组，包含文本和图片
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);



        // 根据智谱AI API要求，image_url应该是一个对象而不是字符串
        message.put("content", new Object[]{textContent});
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
     * 分析问题内容
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果JSON字符串
     * @throws IOException 文件读取或API调用异常
     */
    public ImageResult analyze2(String prompt) throws IOException {
        ZhipuAiClient client = ZhipuAiClient.builder().apiKey(apiKey).build();
        CreateImageRequest request = CreateImageRequest.builder()
                .model(Constants.ModelCogView4250304)
                .prompt(prompt)
                .size("1024x1024")
                .build();
        ImageResponse response = client.images().createImage(request);
        System.out.println(response.getData());
        return response.getData();

    }

    /**
     * 批量试题识别
     * @param imagePaths 图片文件路径列表
     * @return 图片路径与试题识别结果的映射
     * @throws IOException 文件读取或API调用异常
     */
    public Map<String, String> batchRecognizeTitleInImages(List<String> imagePaths) throws IOException {
        String prompt = "请识别分析图片中所有试题的内容，并以文本形式返回，包括大题号、小题号、题内容、题类型、考察知识点、答题是否正确、参考答案、解析。";
        return batchAnalyzeImages(imagePaths, prompt);
    }
    
    /**
     * 批量试题批阅
     * @param imagePaths 图片文件路径列表
     * @return 图片路径与试题批阅结果的映射
     * @throws IOException 文件读取或API调用异常
     */
    public Map<String, String> batchRecognizePiyueInImages(List<String> imagePaths) throws IOException {
        //String prompt = "请识别批阅图片中所有试题的内容，并以文本形式返回 包括大题号（包含题型）、小题号、批阅结果（批阅结果可以是 正确、错误、未答题），不要题内容。";
        String prompt = "请识别批阅图片中所有试题的内容，并以JSON格式返回，格式如[{\"bigNumber\":\"一\",\"questionType\":\"选择题\",\"smallDtoList\":[{{\"smallNumber\":\"1\",\"correctFlag\":\"错误\"}]}] ";
        return batchAnalyzeImages(imagePaths, prompt);
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
     * AI批阅图片试题并返回QuestionAnalysis列表的JSON格式结果
     * @param imagePath 图片文件路径
     * @return QuestionAnalysis列表的JSON字符串
     * @throws IOException 文件读取或API调用异常
     */
    public List<QuestionAnalysis> reviewExamQuestions(String imagePath) throws IOException {
        // 构建详细的批阅提示词，要求模型返回结构化信息
        String prompt = "请详细分析并批阅图片中的所有试题，返回以下信息：\n"
                     + "1. 题号： 包含试题大题号、小题号,格式如大题号：小题号：\n"
                     + "2. 题型： 试题题型类别\n"
                     + "3. 分值： 试题分值\n"
                     + "4. 问题内容：试题原内容\n"
                     + "5. 学生答案：学生提供的答案内容\n"
                     + "6. 参考答案：正确的答案\n"
                     + "7. 批改结果：判断学生答案是否正确\n"
                     + "8. 得分情况：学生实际得分\n"
                     + "9. 错误分析：如果答案错误，请分析错误原因\n"
                     + "10. 考察知识点：该题考察的知识点\n\n"
                     + "请确保为每个试题提供完整的信息，不要用特殊字符（如 学生答案后不要有**等，直接 学生答案：），格式清晰，便于解析。";
        
        // 获取AI分析结果
        String aiResult = analyzeImage(imagePath, prompt);
        System.out.println("AI分析结果:"+aiResult);
        // 解析AI结果为QuestionAnalysis列表
        List<QuestionAnalysis> questionAnalysisList = parseAIResultToQuestionAnalysisList(aiResult);
        return questionAnalysisList;
    }
    
    /**
     * 批量AI批阅图片试题并返回QuestionAnalysis列表的JSON格式结果
     * @param imagePaths 图片文件路径列表
     * @return 图片路径与QuestionAnalysis列表JSON的映射
     * @throws IOException 文件读取或API调用异常
     */
    public List<QuestionAnalysis>  batchReviewExamQuestions(List<String> imagePaths) throws IOException, ExecutionException, InterruptedException {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return null;
        }
        
        // 创建线程池用于并行处理图片
        int threadCount = Math.min(5, imagePaths.size()); // 限制最大线程数
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<QuestionAnalysis> results = new ArrayList<>();
        Map<String, List<QuestionAnalysis>> futureResults = new HashMap<>();
        
        try {
            // 提交所有图片分析任务
            for (String imagePath : imagePaths) {
                final String finalImagePath = imagePath;
                futureResults.put(imagePath, executorService.submit(() -> reviewExamQuestions(finalImagePath)).get());
            }
            
            // 收集所有结果
            for (Map.Entry<String, List<QuestionAnalysis>> entry : futureResults.entrySet()) {
                String imagePath = entry.getKey();
                try {
                    List<QuestionAnalysis> result = entry.getValue(); // 获取分析结果，会阻塞直到完成
                    results.addAll(result);
                } catch (Exception e) {
                    throw new IOException("批阅图片试题失败: " + imagePath, e);
                }
            }
            
            return results;
        } finally {
            executorService.shutdown();
        }
    }
    
    /**
     * 解析AI返回的结果文本，转换为QuestionAnalysis列表
     * @param aiResultText AI返回的结果文本
     * @return QuestionAnalysis对象列表
     */
    public List<QuestionAnalysis> parseAIResultToQuestionAnalysisList(String aiResultText) {
        List<QuestionAnalysis> questionAnalysisList = new ArrayList<>();
        
        try {
            // 按行分割AI结果
            String[] lines = aiResultText.split("\n");
            
            // 当前正在处理的题目
            QuestionAnalysis currentQuestion = null;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 根据关键词识别题目开始
                if (line.contains("大题号")||line.contains("题号")) {
                    // 如果已有正在处理的题目，先添加到列表
                    if (currentQuestion != null) {
                        questionAnalysisList.add(currentQuestion);
                    }
                    
                    // 创建新的题目分析对象
                    currentQuestion = new QuestionAnalysis();
                    
                    // 解析大题号
                    if (line.contains("大题号")) {
                        String bigNumber = extractBetween(line, "大题号", "小题号");
                        bigNumber = startSub(bigNumber,"：");
                        if(bigNumber.contains("，")||bigNumber.contains("；")||bigNumber.contains("、")){
                            bigNumber = bigNumber.substring(0,bigNumber.length()-1);
                        }
                        currentQuestion.setBigNumber(bigNumber);
                    }
                    
                    // 解析小题号
                    if (line.contains("小题号")) {
                        String smallNumber = extractBetween(line, "小题号", "\n|$");
                        smallNumber = startSub(smallNumber,"：");

                        currentQuestion.setSmallNumber(smallNumber);
                    } else if (line.contains("题号")) {
                        String questionNumber = extractBetween(line, "题号", "\s|\n|$");
                        questionNumber = startSub(questionNumber,"：");
                        if("无".equals(questionNumber.trim())){
                            continue;
                        }
                        if(questionNumber.contains("**")){
                            questionNumber = questionNumber.replace("**","");
                        }
                        currentQuestion.setQuestionNumber(questionNumber);
                        if(questionNumber.length()>=3&&questionNumber.length()<=6){
                            currentQuestion.setBigNumber(questionNumber.substring(0,1));
                            currentQuestion.setSmallNumber(questionNumber.substring(2));
                        }
                    }
                }
                
                // 解析其他属性
                if (currentQuestion != null) {
                    // 解析小题号
                    if (line.contains("小题号")) {
                        String smallNumber = extractBetween(line, "小题号", "\n|$");
                        smallNumber = startSub(smallNumber,"：");

                        currentQuestion.setSmallNumber(smallNumber);
                    }
                    if (line.contains("题型")) {
                        String questionType = extractBetween(line, "题型", "\n|$");
                        questionType = startSub(questionType,"：");
                        if(questionType.contains("分值：")){
                            questionType = questionType.substring(0,questionType.indexOf("分值：")-1);
                        }
                        currentQuestion.setQuestionType(questionType);
                    } else if (line.contains("分值")) {
                        String scoreStr = extractBetween(line, "分值", "\n|$");
                        scoreStr = startSub(scoreStr,"：");
                        try {
                            // 提取数字部分
                            scoreStr = scoreStr.replaceAll("\\D+", "");
                            if (!scoreStr.isEmpty()) {
                                currentQuestion.setScore(Integer.parseInt(scoreStr));
                            }
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    } else if (line.contains("问题内容")) {
                        String content = extractBetween(line, "问题内容", "\n|$");
                        content = startSub(content,"：");
                        currentQuestion.setContent(content);
                    }else if (line.contains("学生答案")) {
                        String studentAnswer = extractBetween(line, "学生答案", "\n|$");
                        studentAnswer = startSub(studentAnswer,"：");
                        currentQuestion.setStudentAnswer(studentAnswer);
                    } else if (line.contains("参考答案")) {
                        String referenceAnswer = extractBetween(line, "参考答案", "\n|$");
                        referenceAnswer = startSub(referenceAnswer,"：");
                        currentQuestion.setReferenceAnswer(referenceAnswer);
                    } else if (line.contains("批改结果")) {
                        String reviewResult = extractBetween(line, "批改结果", "\n|$");
                        reviewResult = startSub(reviewResult,"：");
                        if(StringUtils.isNotEmpty(currentQuestion.getStudentAnswer())
                                &&(currentQuestion.getStudentAnswer().contains("未显示")
                                ||currentQuestion.getStudentAnswer().contains("未作答")
                                ||currentQuestion.getStudentAnswer().contains("无"))){
                            currentQuestion.setIsCorrect(null);
                        }else {
                            if (reviewResult.contains("正确") || reviewResult.contains("对")) {
                                currentQuestion.setIsCorrect(true);
                            } else if (reviewResult.contains("错误") || reviewResult.contains("错")) {
                                currentQuestion.setIsCorrect(false);
                            }
                        }

                    } else if (line.contains("得分情况")) {
                        String obtainedScoreStr = extractBetween(line, "得分情况", "\n|$");
                        obtainedScoreStr = startSub(obtainedScoreStr,"：");
                        try {
                            // 提取数字部分
                            obtainedScoreStr = obtainedScoreStr.replaceAll("\\D+", "");
                            if (!obtainedScoreStr.isEmpty()) {
                                currentQuestion.setObtainedScore(Integer.parseInt(obtainedScoreStr));
                            }
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    } else if (line.contains("错误分析")) {
                        String analysis = extractBetween(line, "错误分析", "\n|$");
                        analysis = startSub(analysis,"：");
                        currentQuestion.setAnalysis(analysis);
                    } else if (line.contains("考察知识点")) {
                        String knowledgePoints = extractBetween(line, "考察知识点", "\n|$");
                        knowledgePoints = startSub(knowledgePoints,"：");
                        currentQuestion.setKnowledgePoints(knowledgePoints);
                    }
                }
            }
            
            // 添加最后一个题目
            if (currentQuestion != null) {
                questionAnalysisList.add(currentQuestion);
            }
            
        } catch (Exception e) {
            // 如果解析失败，记录错误并返回空列表
            System.err.println("解析AI批阅结果失败: " + e.getMessage());
        }
        
        return questionAnalysisList;
    }
    
    /**
     * 从文本中提取指定标记之间的内容，支持正则表达式作为结束标记
     * @param text 原始文本
     * @param startTag 开始标记
     * @param endTag 结束标记（正则表达式）
     * @return 提取的内容
     */
    private String extractBetween(String text, String startTag, String... endTags) {
        int startIndex = text.indexOf(startTag);
        if (startIndex == -1) return "";
        
        startIndex += startTag.length();
        int minEndIndex = text.length();
        
        for (String endTag : endTags) {
            if (endTag.contains("|")) {
                // 处理或条件
                String[] options = endTag.split("\\|");
                for (String option : options) {
                    if (!option.isEmpty()) {
                        int endIndex = text.indexOf(option, startIndex);
                        if (endIndex != -1 && endIndex < minEndIndex) {
                            minEndIndex = endIndex;
                        }
                    }
                }
            } else {
                int endIndex = text.indexOf(endTag, startIndex);
                if (endIndex != -1 && endIndex < minEndIndex) {
                    minEndIndex = endIndex;
                }
            }
        }
        
        if (minEndIndex == text.length()) {
            return text.substring(startIndex).trim();
        }
        
        return text.substring(startIndex, minEndIndex).trim();
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
     * 解析API响应并返回JSON格式的结构化结果
     * @param responseBody API返回的原始响应
     * @return JSON格式的结构化结果
     * @throws IOException JSON解析异常
     */
    private String parseAndFormatResponseToJson(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        // 检查是否有error字段
        if (rootNode.has("error")) {
            JsonNode errorNode = rootNode.get("error");
            String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "未知错误";
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", errorMessage);
            return objectMapper.writeValueAsString(errorResult);
        }
        
        // 提取choices中的内容
        if (rootNode.has("choices") && !rootNode.get("choices").isEmpty()) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                String content = firstChoice.get("message").get("content").asText();
                
                // 构建结构化JSON响应
                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("timestamp", new Date().toString());
                result.put("content", content);
                
                // 添加模型信息
                if (rootNode.has("model")) {
                    result.put("model", rootNode.get("model").asText());
                }
                
                // 添加token使用情况
                if (rootNode.has("usage")) {
                    Map<String, Object> usageInfo = new HashMap<>();
                    JsonNode usageNode = rootNode.get("usage");
                    if (usageNode.has("prompt_tokens")) usageInfo.put("prompt_tokens", usageNode.get("prompt_tokens").asInt());
                    if (usageNode.has("completion_tokens")) usageInfo.put("completion_tokens", usageNode.get("completion_tokens").asInt());
                    if (usageNode.has("total_tokens")) usageInfo.put("total_tokens", usageNode.get("total_tokens").asInt());
                    result.put("usage", usageInfo);
                }
                
                return objectMapper.writeValueAsString(result);
            }
        }
        
        // 如果没有找到预期的内容格式，返回结构化的原始响应
        Map<String, Object> fallbackResult = new HashMap<>();
        fallbackResult.put("status", "unexpected_format");
        fallbackResult.put("timestamp", new Date().toString());
        fallbackResult.put("raw_response", responseBody);
        return objectMapper.writeValueAsString(fallbackResult);
    }
    
    /**
     * 分析图片内容并返回JSON格式的结构化结果
     * @param imagePath 图片文件路径
     * @param prompt 提示词，指导模型如何分析图片
     * @return JSON格式的结构化分析结果
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeImageToJson(String imagePath, String prompt) throws IOException {
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
        
        // 5. 处理响应，返回JSON格式结果
        return parseAndFormatResponseToJson(response.getBody());
    }
    
    /**
     * 分析文本内容并返回JSON格式的结构化结果
     * @param prompt 提示词
     * @return JSON格式的结构化分析结果
     * @throws IOException API调用异常
     */
    public String analyzeToJson(String prompt) throws IOException {
        // 1. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "glm-4.5v"); // 使用GLM-4V多模态模型
        
        // 构建messages参数
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        
        // 构建content数组，包含文本
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        
        message.put("content", new Object[]{textContent});
        requestBody.put("messages", new Object[]{message});
        
        // 2. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");
        
        // 3. 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "chat/completions", request, String.class);
        
        // 4. 处理响应，返回JSON格式结果
        return parseAndFormatResponseToJson(response.getBody());
    }
    
    /**
     * 解析文本形式的AI结果为结构化的ExamPaperAnalysis对象
     * @param aiResultText AI返回的文本结果
     * @return 结构化的试卷分析对象
     */
    public ExamPaperAnalysis parseTextToExamPaperAnalysis(String aiResultText) {
        ExamPaperAnalysis analysis = new ExamPaperAnalysis();
        
        // 这里可以根据实际的AI返回格式实现解析逻辑
        // 以下是一个简单的示例实现
        try {
            // 尝试解析试卷名称
            if (aiResultText.contains("试卷名称:")) {
                String paperName = extractBetween(aiResultText, "试卷名称:", "\n");
                analysis.setPaperName(paperName);
            }
            
            // 尝试解析总页数
            if (aiResultText.contains("总页数:")) {
                String totalPagesStr = extractBetween(aiResultText, "总页数:", "\n");
                try {
                    analysis.setTotalPages(Integer.parseInt(totalPagesStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            // 尝试解析总题数
            if (aiResultText.contains("总题数:")) {
                String totalQuestionsStr = extractBetween(aiResultText, "总题数:", "\n");
                try {
                    analysis.setTotalQuestions(Integer.parseInt(totalQuestionsStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            // 尝试解析总分值
            if (aiResultText.contains("总分值:")) {
                String totalScoreStr = extractBetween(aiResultText, "总分值:", "\n");
                try {
                    analysis.setTotalScore(Integer.parseInt(totalScoreStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            // 尝试解析总体评价
            if (aiResultText.contains("总体评价:")) {
                String overallEvaluation = extractBetween(aiResultText, "总体评价:", "\n");
                analysis.setOverallEvaluation(overallEvaluation);
            }
            
            // 此处可以添加更多解析逻辑，例如解析每个试题的信息
            // 由于AI返回格式可能不固定，这里只做基本解析
            
        } catch (Exception e) {
            // 忽略解析错误，返回基本对象
            System.err.println("解析AI结果为结构化数据时出错: " + e.getMessage());
        }
        
        return analysis;
    }
    
    /**
     * 从文本中提取指定标记之间的内容
     * @param text 原始文本
     * @param startTag 开始标记
     * @param endTag 结束标记
     * @return 提取的内容
     */
    private String extractBetween(String text, String startTag, String endTag) {
        int startIndex = text.indexOf(startTag);
        if (startIndex == -1) return "";
        
        startIndex += startTag.length();
        int endIndex = text.indexOf(endTag, startIndex);
        if (endIndex == -1) return text.substring(startIndex).trim();
        
        return text.substring(startIndex, endIndex).trim();
    }
    private String startSub(String text, String startTag){
        if(text.contains(startTag)){
            int startIndex = text.indexOf(startTag)+1;
            return text.substring(startIndex).trim();
        }else{
            return text;
        }
    }

    /**
     * 创建带配置参数的工具实例
     * @param apiKey 智谱AI的API密钥
     * @return 工具实例
     */
    public static ZhipuAIImageAnalysisUtil createInstance(String apiKey) {
        return new ZhipuAIImageAnalysisUtil(apiKey);
    }

    public static void main(String[] args) {
        String aiText ="<|begin_of_box|>### 一、填空题  \n" +
                "#### 小题1  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：1  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：2分  \n" +
                "- **问题内容**：一名九年级毕业生在体育体能测试中，实心球的成绩是9.06米，读作：（ ）；它是由9个一和6个（ ）组成的。  \n" +
                "- **学生答案**：九点零六；0.01  \n" +
                "- **参考答案**：九点零六；0.01  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：2分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：小数的读法、数的组成  \n" +
                "\n" +
                "\n" +
                "#### 小题2  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：2  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：3分  \n" +
                "- **问题内容**：在巴西举行的世界杯上，共有3869000人到现场看比赛，横线上的数改写成用“万”作单位的数是（ ）万人；共有4239820000人观看了开幕式，横线上的数改写成用“亿”作单位的数并保留两位小数约是（ ）亿人。  \n" +
                "- **学生答案**：386.9；42.40  \n" +
                "- **参考答案**：386.9；42.40  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：3分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：整数的改写、求近似数  \n" +
                "\n" +
                "\n" +
                "#### 小题3  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：3  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：2分  \n" +
                "- **问题内容**：1元人民币可以换0.1271欧元，1万元人民币可以换（ ）欧元。  \n" +
                "- **学生答案**：1271  \n" +
                "- **参考答案**：1271  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：2分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：小数乘法的实际应用  \n" +
                "\n" +
                "\n" +
                "#### 小题4  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：4  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：2分  \n" +
                "- **问题内容**：小丽画了一个三角形，其中两条边分别是7cm和4cm，那么第三条边可能是（ ）。（答案不唯一，只要满足3cm＜第三边＜11cm）  \n" +
                "- **学生答案**：5cm（假设答案在3cm～11cm范围内）  \n" +
                "- **参考答案**：5cm（答案不唯一，如5、6、7、8、9、10均符合条件）  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：2分  \n" +
                "- **错误分析**：无（若答案在范围内）  \n" +
                "- **考察知识点**：三角形三边关系定理  \n" +
                "\n" +
                "\n" +
                "#### 小题5  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：5  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：3分  \n" +
                "- **问题内容**：如右图，量一量，想一想，按角分这个三角形是（ ）三角形，理由是（ ）；按边分，这个三角形是（ ）三角形。（假设图中三角形为锐角且三边相等）  \n" +
                "- **学生答案**：锐角；三个角都是锐角；等边  \n" +
                "- **参考答案**：锐角；三个角都是锐角；等边  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：3分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：三角形的分类（按角和按边）  \n" +
                "\n" +
                "\n" +
                "#### 小题6  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：6  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：2分  \n" +
                "- **问题内容**：一个三角形中最多有（ ）个锐角，最多有（ ）个直角，最多有（ ）个钝角。  \n" +
                "- **学生答案**：3；1；1  \n" +
                "- **参考答案**：3；1；1  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：2分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：三角形内角和及角的类型  \n" +
                "\n" +
                "\n" +
                "#### 小题7  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：7  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：2分  \n" +
                "- **问题内容**：至少用（ ）个完全一样的三角形就能拼成一个平行四边形。  \n" +
                "- **学生答案**：2  \n" +
                "- **参考答案**：2  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：2分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：图形拼组（三角形与平行四边形的关系）  \n" +
                "\n" +
                "\n" +
                "#### 小题8  \n" +
                "- **大题号**：一  \n" +
                "- **小题号**：8  \n" +
                "- **题型**：填空题  \n" +
                "- **分值**：4分  \n" +
                "- **问题内容**：(1)每100粒米重（ ）克。(2)平均1粒米重（ ）克，这个数读作（ ）克。(3)2016年我国有在校小学生九千六百九十二点二万，横线上的数写作（ ），改写成以“亿”为单位的数保留到整数约是（ ）亿。  \n" +
                "- **学生答案**：(1)12；(2)0.12；零点一二；(3)9692.2万；1  \n" +
                "- **参考答案**：(1)12；(2)0.12；零点一二；(3)96922000；1（注：九千六百九十二点二万写作9692.2万，即96922000；改写成亿是0.96922亿≈1亿）  \n" +
                "- **批改结果**：正确  \n" +
                "- **得分情况**：4分  \n" +
                "- **错误分析**：无  \n" +
                "- **考察知识点**：质量计算、小数的读写、大数的写法与近似数<|end_of_box|>";
        ZhipuAIImageAnalysisUtil util = createInstance("7abc333508dd4d71b83dcb6f5a511eee.rjwsPZyUfDN5abhn");
        List<QuestionAnalysis> analyses=util.parseAIResultToQuestionAnalysisList(aiText);
        System.out.println(analyses.toString());
    }
}