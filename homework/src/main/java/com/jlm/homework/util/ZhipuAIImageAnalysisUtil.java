package com.jlm.homework.util;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.core.Constants;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import ai.z.openapi.service.image.ImageResult;
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
     * 内部类：试卷分析结果
     * 用于封装整份试卷的分析信息
     */
    public static class ExamPaperAnalysis {
        private String paperName;          // 试卷名称
        private int totalPages;            // 总页数
        private int totalQuestions;        // 总题数
        private int totalScore;            // 总分值
        private List<QuestionAnalysis> questions; // 试题列表
        private String overallEvaluation;  // 总体评价
        private String gradingSummary;     // 评分总结
        private String suggestions;        // 改进建议
        
        // 构造函数
        public ExamPaperAnalysis() {
            this.questions = new ArrayList<>();
        }
        
        // getter和setter方法
        public String getPaperName() { return paperName; }
        public void setPaperName(String paperName) { this.paperName = paperName; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        
        public List<QuestionAnalysis> getQuestions() { return questions; }
        public void setQuestions(List<QuestionAnalysis> questions) { this.questions = questions; }
        
        public String getOverallEvaluation() { return overallEvaluation; }
        public void setOverallEvaluation(String overallEvaluation) { this.overallEvaluation = overallEvaluation; }
        
        public String getGradingSummary() { return gradingSummary; }
        public void setGradingSummary(String gradingSummary) { this.gradingSummary = gradingSummary; }
        
        public String getSuggestions() { return suggestions; }
        public void setSuggestions(String suggestions) { this.suggestions = suggestions; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("试卷分析结果：\n")
              .append("试卷名称: ").append(paperName).append("\n")
              .append("总页数: ").append(totalPages).append("\n")
              .append("总题数: ").append(totalQuestions).append("\n")
              .append("总分值: ").append(totalScore).append("\n")
              .append("试题详情: \n");
              
            for (QuestionAnalysis q : questions) {
                sb.append("  - ").append(q.toString()).append("\n");
            }
            
            sb.append("总体评价: ").append(overallEvaluation).append("\n")
              .append("评分总结: ").append(gradingSummary).append("\n")
              .append("改进建议: ").append(suggestions);
              
            return sb.toString();
        }
    }
    
    /**
     * 内部类：试题分析结果
     */
    public static class QuestionAnalysis {
        private String questionNumber;     // 题号
        private String questionType;       // 题型
        private String content;            // 题目内容
        private int score;                 // 分值
        private String studentAnswer;      // 学生答案
        private String referenceAnswer;    // 参考答案
        private int obtainedScore;         // 得分
        private String analysis;           // 分析
        private String knowledgePoints;    // 考察知识点
        
        // getter和setter方法
        public String getQuestionNumber() { return questionNumber; }
        public void setQuestionNumber(String questionNumber) { this.questionNumber = questionNumber; }
        
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        
        public String getStudentAnswer() { return studentAnswer; }
        public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
        
        public String getReferenceAnswer() { return referenceAnswer; }
        public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
        
        public int getObtainedScore() { return obtainedScore; }
        public void setObtainedScore(int obtainedScore) { this.obtainedScore = obtainedScore; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public String getKnowledgePoints() { return knowledgePoints; }
        public void setKnowledgePoints(String knowledgePoints) { this.knowledgePoints = knowledgePoints; }
        
        @Override
        public String toString() {
            return questionNumber + " [" + questionType + "] " + score + "分 - 得分: " + obtainedScore;
        }
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
     * @return 分析结果JSON字符串
     * @throws IOException 文件读取或API调用异常
     */
    public String analyzeImage(String imagePath, String prompt) throws IOException {
        // 1. 读取图片并进行Base64编码
        String base64Image = encodeImageToBase64(imagePath);
        
        // 2. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "glm-4.6v"); // 使用GLM-4V多模态模型
        
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
     * 分析图片内容
     * @param imagePath 图片文件路径
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果JSON字符串
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
     * @param prompt 提示词，指导模型如何分析图片
     * @return 分析结果JSON字符串
     * @throws IOException 文件读取或API调用异常
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
        String prompt = "请识别批阅图片中所有试题的内容，并以文本形式返回 包括大题号（包含题型）、小题号、批阅结果（批阅结果可以是 正确、错误、未答题），不要题内容。";
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
            String imagePath = "E:\\jlm\\jlm-server-finally\\10月21日数学作业二_李强_1页作业.png";
            // 规范化URL路径
            String normalizedPath = imagePath.replace("\\", "/")
                    .replace("http:/", "http://");

            // 测试单张图片分析
            // 测试图片描述
            //String description = util.describeImage(normalizedPath);
            //System.out.println("图片描述: " + description);
            
            // 测试文字识别
            //String text = util.recognizeTextInImage(imagePath);
            //System.out.println("识别的文字: " + text);

            // 试题识别
            //String titleText = util.recognizePiyueInImage(imagePath);
            //System.out.println("试题内容: " + titleText);
            
            // 测试批量图片分析
            List<String> imagePaths = new ArrayList<>();
            imagePaths.add(imagePath);
            // 如果有更多图片，可以继续添加
            // imagePaths.add("第二张图片路径");
            // imagePaths.add("第三张图片路径");
            
            // 批量试题识别测试
            System.out.println("开始批量试题识别测试...");
            Map<String, String> batchResults = util.batchRecognizeTitleInImages(imagePaths);
            for (Map.Entry<String, String> entry : batchResults.entrySet()) {
                System.out.println("图片路径: " + entry.getKey());
                System.out.println("分析结果: " + entry.getValue());
                System.out.println("---------------------------");
            }
            
            // 批量试题批阅测试
            System.out.println("开始批量试题批阅测试...");
            Map<String, String> batchPiyueResults = util.batchRecognizePiyueInImages(imagePaths);
            for (Map.Entry<String, String> entry : batchPiyueResults.entrySet()) {
                System.out.println("图片路径: " + entry.getKey());
                System.out.println("批阅结果: " + entry.getValue());
                System.out.println("---------------------------");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}