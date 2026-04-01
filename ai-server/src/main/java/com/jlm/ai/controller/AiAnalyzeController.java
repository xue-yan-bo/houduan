package com.jlm.ai.controller;

import com.jlm.ai.dto.Result;
import com.jlm.ai.dto.SimilarityRequestDto;
import com.jlm.ai.dto.SimilarityResultDto;
import com.jlm.ai.util.AIUtil;
import com.jlm.ai.util.TextSimilarityUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/ai")
public class AiAnalyzeController {

    @Resource
    private AIUtil aiUtil;

    /**
     * RPC 接口：提供图片试题文字提取服务
     * 作用：把原来散落在 `extractImageTextIfEmpty` 里面那段需要阻塞十几秒去调用大模型的代码独立出来。
     * @param imageUrl 图片公网地址
     * @return 提取出来的纯文本
     */
    @PostMapping("/analyzeImage")
    public Result<String> analyzeImage(@RequestParam("imageUrl") String imageUrl) {
        if (StringUtils.isEmpty(imageUrl)) {
            return Result.success("");
        }

        try {
            String prompt = "请提取这张图片中的所有试题文字内容（包含题目、选项、解析等）。不论是文科（语文、历史、英语等）、理科还是美术等其他学科，请忠实还原图片中的所有文字。如果包含公式或特殊符号，请尽量使用Markdown或LaTeX语法表示。只返回提取的纯文字内容，不要输出诸如好的、提取的文字如下等任何废话。如果识别不到文字，只需返回空字符串。";
            // 拿到具体的工厂类实现去解析
            String text = aiUtil.getAIUtil().analyzeImage(imageUrl, prompt);

            // 打印一下大模型返回结果，方便我们在控制台排查
            System.out.println("====== 大模型返回提取文字 ======\n" + text);

            return Result.success("提取成功", text);
        } catch (Exception e) {
            System.err.println("AI提取题目文字失败: " + e.getMessage());
            // 如果报错也别卡死流程，直接返回空字符串，让业务端正常当新题入库就行了
            return Result.success("");
        }
    }

    /**
     * RPC 接口：高密度 CPU 相似度查重服务
     * 作用：让 `aiChart` 异步线程里的循环查重不要在业务系统里算，放到这里来算
     * @param request 查重实体 (当前新题内容 + 长长的历史错题列表集合)
     * @return SimilarityResultDto 返回最高相似度和命中那道题的ID
     */
    @PostMapping("/checkSimilarity")
    public Result<SimilarityResultDto> checkSimilarity(@RequestBody SimilarityRequestDto request) {
        SimilarityResultDto resultDto = new SimilarityResultDto();
        resultDto.setMaxSimilarity(0.0);

        if (request == null || StringUtils.isEmpty(request.getTargetText()) || request.getHistoryTexts() == null || request.getHistoryTexts().isEmpty()) {
            return Result.success("空数据无需查重", resultDto);
        }

        String targetText = request.getTargetText();
        double maxSimilarity = 0.0;
        Long mostSimilarBookId = null;

        // 极其耗费 CPU 的 for 循环编辑距离比对（这就是为什么要拆出微服务的原因）
        for (SimilarityRequestDto.HistoryTextDto history : request.getHistoryTexts()) {
            if (StringUtils.isEmpty(history.getText())) {
                continue;
            }
            double similarity = TextSimilarityUtil.getSimilarity(targetText, history.getText());
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarBookId = history.getId();
            }
        }

        resultDto.setMaxSimilarity(maxSimilarity);
        resultDto.setDuplicateOf(mostSimilarBookId);

        return Result.success("查重计算完毕", resultDto);
    }
}