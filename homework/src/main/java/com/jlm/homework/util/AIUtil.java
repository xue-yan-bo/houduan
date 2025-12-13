package com.jlm.homework.util;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.entity.AiConfig;
import com.jlm.homework.entity.HomeworkPublishQuestion;
import com.jlm.homework.entity.QuestionAnalysis;
import com.jlm.homework.repository.AiConfigRepository;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Data
public abstract class AIUtil {
    @Resource
    private AiConfigRepository aiConfigRepository;
    @Autowired
    private ZhipuAIConfig zhipuAIConfig;
    @Resource
    private TongYiServ tongYiServ;

    private String aiName;
    public AIUtil getAIUtil(){
        AIUtil util = null;
        AiConfig search = new AiConfig();
        search.setUseFlag(1);
        AiConfig aiConfig = aiConfigRepository.findOne(Example.of(search)).orElse(null);
        
        if (aiConfig != null && "qianwen".equals(aiConfig.getAiName())) {
            util = new QianWenAIUtil();
            util.setAiName(aiConfig.getAiName());
            // 复制当前实例的依赖项到新创建的实例
            util.setAiConfigRepository(this.aiConfigRepository);
            util.setZhipuAIConfig(this.zhipuAIConfig);
            // 设置TongYiServ依赖
            util.setTongYiServ(this.tongYiServ);
        } else {
            util = zhipuAIConfig.zhipuAIImageAnalysisUtil();
            util.setAiName(aiConfig != null ? aiConfig.getAiName() : "zhipu");
            // 复制当前实例的依赖项到新创建的实例
            util.setAiConfigRepository(this.aiConfigRepository);
            util.setZhipuAIConfig(this.zhipuAIConfig);
        }
        
        return util;
    }


    public abstract Map<String, String> analyzeImagesAnswer(List<String> imageNames) throws IOException, NoApiKeyException, InputRequiredException;

    public abstract String analyzeImage(String outputPath, String prompt) throws IOException, NoApiKeyException, InputRequiredException;

    public abstract Map<String, String> batchRecognizePiyueInImages(List<String> imageNames) throws IOException;

    public abstract List<QuestionAnalysis> batchReviewExamQuestions(List<String> imageNames) throws IOException, ExecutionException, InterruptedException;

    public abstract List<HomeworkPublishQuestion> reviewHomreWorkQuestions(List<String> imageNames) throws IOException;

    public abstract String analyzeText(String pamt) throws IOException, NoApiKeyException, InputRequiredException;
}
