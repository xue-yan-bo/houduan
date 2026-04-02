package com.jlm.ai.util;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.jlm.ai.dto.HomeworkPublishQuestionDto;
import com.jlm.ai.dto.QuestionAnalysisDto;
import com.jlm.ai.config.ZhipuAIConfig;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Data
public abstract class AIUtil {
    @Resource
    @Autowired
    private ZhipuAIConfig zhipuAIConfig;
    @Resource
    private TongYiServ tongYiServ;

    @org.springframework.beans.factory.annotation.Value("${ai.provider:zhipu}")
    private String aiName;

    public AIUtil getAIUtil(){
        AIUtil util = null;

        if ("qianwen".equals(aiName)) {
            util = new QianWenAIUtil();
            util.setAiName(aiName);
            // 复制当前实例的依赖项到新创建的实例
            util.setZhipuAIConfig(this.zhipuAIConfig);
            // 设置TongYiServ依赖
            util.setTongYiServ(this.tongYiServ);
        } else {
            util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
            util.setAiName(aiName);
            // 复制当前实例的依赖项到新创建的实例
            util.setZhipuAIConfig(this.zhipuAIConfig);
        }

        return util;
    }


    public abstract Map<String, String> analyzeImagesAnswer(List<String> imageNames) throws IOException, NoApiKeyException, InputRequiredException;

    public abstract String analyzeImage(String outputPath, String prompt) throws IOException, NoApiKeyException, InputRequiredException;

    public abstract Map<String, String> batchRecognizePiyueInImages(List<String> imageNames) throws IOException;

    public abstract List<QuestionAnalysisDto> batchReviewExamQuestions(List<String> imageNames) throws IOException, ExecutionException, InterruptedException;

    public abstract List<HomeworkPublishQuestionDto> reviewHomreWorkQuestions(List<String> imageNames) throws IOException;

    public abstract String analyzeText(String pamt) throws IOException, NoApiKeyException, InputRequiredException;
}
