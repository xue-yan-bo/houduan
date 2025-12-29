package com.jlm.homework.util;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.fastjson.JSON;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.entity.HomeworkPublishQuestion;
import com.jlm.homework.entity.QuestionAnalysis;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class QianWenAIUtil extends AIUtil{
    private static String DASHSCOPE_API_KEY = "sk-234cb378f4484f3e9f6fc5b8304508e5";
    private ZhipuAIConfig zhipuAIConfig = new ZhipuAIConfig();
    /**
     * 分析问题内容
     * @param prompt 提示词，指导模型如何分析
     * @return 分析结果文本
     * @throws IOException API调用异常
     */
    public static Object[] content(String prompt) throws IOException {
        // 构建content数组，包含文本和图片
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        return new Object[]{textContent};
    }

    /**
     * 分析问题内容
     * @param prompt 提示词，指导模型如何分析
     * @return 分析结果文本
     * @throws IOException API调用异常
     */
    public static Object[] contentImage(String imagePath,String prompt) throws IOException {
        // 构建content数组，包含文本和图片
        // 构建content数组，包含文本和图片
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");

        // 根据智谱AI API要求，image_url应该是一个对象而不是字符串
        Map<String, String> imageUrlObject = new HashMap<>();
        imageUrlObject.put("url", imagePath);
        imageContent.put("image_url", imageUrlObject);
        return new Object[]{textContent, imageContent};
    }
    /**
     * 分析问题内容
     * @param prompt 提示词，指导模型如何分析
     * @return 分析结果文本
     * @throws IOException API调用异常
     */
    public static Object[] contentImages(List<String> imagePaths,String prompt) throws IOException {
        // 构建content数组，包含文本和图片
        // 构建content数组，包含文本和图片
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        Object[] objects=new Object[imagePaths.size()+1];
        objects[0]=textContent;
        for(int i=0;i<imagePaths.size();i++) {
            String imagePath = imagePaths.get(0);
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");

            // 根据智谱AI API要求，image_url应该是一个对象而不是字符串
            Map<String, String> imageUrlObject = new HashMap<>();
            imageUrlObject.put("url", imagePath);
            imageContent.put("image_url", imageUrlObject);
            objects[i+1]=imageContent;
        }
        return objects;
    }
    public static GenerationResult callWithMessage(String question) throws ApiException, NoApiKeyException, InputRequiredException, IOException {
        Generation gen = new Generation(Protocol.HTTP.getValue(), "https://dashscope.aliyuncs.com/api/v1");
        /*Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();*/
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(JSON.toJSONString(content(question)))
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(DASHSCOPE_API_KEY)
                .model("qwen3-max")
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        return gen.call(param);
    }

    public static GenerationResult callWithMessage(String imagePath,String prompt) throws ApiException, NoApiKeyException, InputRequiredException, IOException {
        Generation gen = new Generation(Protocol.HTTP.getValue(), "https://dashscope.aliyuncs.com/api/v1");
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(JSON.toJSONString(contentImage(imagePath,prompt)))
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(DASHSCOPE_API_KEY)
                .model("qwen3-max")
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        return gen.call(param);
    }

    /**
     * AI批阅图片试题并返回QuestionAnalysis列表的JSON格式结果
     * @param imagePath 图片文件路径
     * @return QuestionAnalysis列表的JSON字符串
     * @throws IOException 文件读取或API调用异常
     */
    public List<QuestionAnalysis> reviewExamQuestions(String imagePath) throws IOException, NoApiKeyException, InputRequiredException {
        // 构建详细的批阅提示词，要求模型返回结构化信息
        String prompt = "请依次详细分析并批阅所有图片中的所有试题，按题号顺序返回以下信息：\n"
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
       /* GenerationResult result = callWithMessage(imagePath, prompt);
        String aiResult=result.getOutput().getChoices().get(0).getMessage().getContent();*/
        String aiResult=getTongYiServ().multiModalCall(prompt,Arrays.asList(imagePath));
        //System.out.println("AI分析结果:"+aiResult);
        // 解析AI结果为QuestionAnalysis列表
        ZhipuAIImageAnalysisUtil analysisUtil=zhipuAIConfig.zhipuAIImageAnalysisUtil();
        List<QuestionAnalysis> questionAnalysisList = analysisUtil.parseAIResultToQuestionAnalysisList(aiResult);
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

    @Override
    public List<HomeworkPublishQuestion> reviewHomreWorkQuestions(List<String> imageNames) throws IOException {
        // 构建详细的批阅提示词，要求模型返回结构化信息
        String prompt = "作为一个图片试题分析助手,请依次详细分析并批阅所有图片中的所有试题，按题号顺序返回以下信息：\n"
                + "1. 题号： 包含试题大题号、小题号,格式如大题号：小题号：\n"
                + "2. 题型： 试题题型类别\n"
                + "3. 分值： 试题分值\n"
                + "4. 问题内容：试题原内容\n"
                + "5. 参考答案：正确的答案\n"
                + "6. 考察知识点：该题考察的知识点\n\n"
                + "请严格按照格式分析，确保为每个试题提供完整的信息，大题号必须有，不要用特殊字符（如 问题内容后不要有**等，直接 问题内容：），格式清晰，便于解析。";

        // 获取AI分析结果
        String aiResult = getTongYiServ().multiModalCall(prompt,imageNames);
        //System.out.println("AI分析结果:"+aiResult);
        // 解析AI结果为QuestionAnalysis列表
        ZhipuAIImageAnalysisUtil analysisUtil=zhipuAIConfig.zhipuAIImageAnalysisUtil();
        List<HomeworkPublishQuestion> questionAnalysisList = analysisUtil.parseAIResultToHomreWorkQuestionList(aiResult);
        return questionAnalysisList;
    }

    @Override
    public String analyzeText(String pamt) throws IOException, NoApiKeyException, InputRequiredException {
        return this.analyzeImage(null,pamt);
    }

    public Map<String,String> analyzeImagesAnswer(String imagePath) throws IOException, NoApiKeyException, InputRequiredException {
        String prompt = "你是一个专业的识图助手，请依次详细分析并批阅所有图片，只做识图操作，禁止解答所有题目，只提取学生用蓝色笔手写的文字、数字、符号、选项序号（排除印刷体文字），所有题目提取内容直接转录原始内容，不要包含题目原文、选项文字，严格按以下固定格式输出，按顺序列出图中所有题目，若某题学生未填写则标注 “未作答”。\n"
                + "请严格按以下固定格式输出：\n "
                +"【一级标题（与试卷板块一致，如 “一、填空”）】题目 1：学生手写内容（原样记录字迹 / 符号）\n"
                +"【一级标题（与试卷板块一致，如 “一、填空”）】题目 2：学生手写内容 \n "
                +"…… \n"
                +"【一级标题（如 “二、判断”）】题目 1：学生手写内容\n "
                +"【一级标题（如 “二、判断”）】题目 2：学生手写内容\n "
                +"……\n "
                +"【一级标题（如 “二、判断”）】题目 1：学生手写内容\n "
                +"【一级标题（如 “二、判断”）】题目 2：学生手写内容\n "
                +"注：若学生某题未作答，标注 “学生未作答”；若书写模糊无法识别，标注 “学生书写模糊无法识别”；必须严格匹配试卷题目顺序，不调整、不增删任何内容。\n "
                +"请基于上述要求，提取目标试卷的学生作答笔迹";
       /* GenerationResult result = callWithMessage(imagePath,prompt);
        String aiResult = result.getOutput().getChoices().get(0).getMessage().getContent();*/
        String aiResult=getTongYiServ().multiModalCall(prompt,Arrays.asList(imagePath));
        ZhipuAIImageAnalysisUtil analysisUtil=zhipuAIConfig.zhipuAIImageAnalysisUtil();
        Map<String,String> map = analysisUtil.parseAIResultToMap(aiResult);
        return map;
    }
    public Map<String,String> analyzeImagesAnswer(List<String> imagePaths) throws IOException, NoApiKeyException, InputRequiredException {
        String prompt = "你是一个专业的识图助手，请依次详细分析并批阅所有图片，只做识图操作，禁止解答所有题目，只提取学生用蓝色笔手写的文字、数字、符号、选项序号（排除印刷体文字），所有题目提取内容直接转录原始内容，不要包含题目原文、选项文字，严格按以下固定格式输出，按顺序列出图中所有题目，若某题学生未填写则标注 “未作答”。\n"
                + "请严格按以下固定格式输出：\n "
                +"【一级标题（与试卷板块一致，如 “一、填空”）】题目 1：学生手写内容（原样记录字迹 / 符号）\n"
                +"【一级标题（与试卷板块一致，如 “一、填空”）】题目 2：学生手写内容 \n "
                +"…… \n"
                +"【一级标题（如 “二、判断”）】题目 1：学生手写内容\n "
                +"【一级标题（如 “二、判断”）】题目 2：学生手写内容\n "
                +"……\n "
                +"【一级标题（如 “二、判断”）】题目 1：学生手写内容\n "
                +"【一级标题（如 “二、判断”）】题目 2：学生手写内容\n "
                +"注：若学生某题未作答，标注 “学生未作答”；若书写模糊无法识别，标注 “学生书写模糊无法识别”；必须严格匹配试卷题目顺序，不调整、不增删任何内容。\n "
                +"请基于上述要求，提取目标试卷的学生作答笔迹";
       /* GenerationResult result = callWithMessage(imagePath,prompt);
        String aiResult = result.getOutput().getChoices().get(0).getMessage().getContent();*/
        String aiResult=getTongYiServ().multiModalCall(prompt,imagePaths);
        //System.out.println(aiResult);
        ZhipuAIImageAnalysisUtil analysisUtil=zhipuAIConfig.zhipuAIImageAnalysisUtil();
        Map<String,String> map = analysisUtil.parseAIResultToMap(aiResult);
        //System.out.println(map.toString());
        return map;
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
                futureResults.put(imagePath, executorService.submit(() -> {
                    GenerationResult result=callWithMessage(finalImagePath, prompt);
                    return result.getOutput().getChoices().get(0).getMessage().getContent();
                }));
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

    public String analyzeImage(String imageUrl, String prompt) throws NoApiKeyException, InputRequiredException, IOException {

        /*GenerationResult result=callWithMessage(imageUrl,prompt);
        return result.getOutput().getChoices().get(0).getMessage().getContent();*/
        String aiResult = null;
        if(StringUtils.isNotEmpty(imageUrl)) {
            aiResult = getTongYiServ().multiModalCall(prompt, Arrays.asList(imageUrl));
        }else{
            aiResult = getTongYiServ().multiModalCall(prompt, null);
        }
        return aiResult;
    }
}
